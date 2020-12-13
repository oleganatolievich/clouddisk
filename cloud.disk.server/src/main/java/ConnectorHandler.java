import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import javafx.scene.control.Alert;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConnectorHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE,
        CONTENT_LENGTH,
        CONTENT,
        FILE_LENGTH,
        FILE
    }

    private final Server server;
    private final Settings serverSettings;
    private State currentState = State.IDLE;
    private OperationType currentOperation;
    private long partLength;
    private int currentPosition;
    private byte[] currentData;
    private Path userFilesRoot;
    private Path destFilePath;
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private UploadRequest curUploadRequest;

    public ConnectorHandler(Server server) {
        this.server = server;
        this.serverSettings = server.getSettings();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                byte readed = buf.readByte();
                currentOperation = OperationType.getOperation(readed);
                if (currentOperation != null) {
                    currentState = State.CONTENT_LENGTH;
                    System.out.printf("SERVER STATE: %s, waiting for content length%n", currentOperation.toString());
                } else System.out.printf("SERVER ERROR: Couldn't find operation for signal byte: %d%n", readed);
            }

            if (currentState == State.CONTENT_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    currentPosition = 0;
                    partLength = buf.readLong();
                    currentData = new byte[(int)partLength];
                    currentState = State.CONTENT;
                    System.out.printf("SERVER STATE: Content length is %d bytes%n", partLength);
                }
            }

            if (currentState == State.CONTENT) {
                int readableBytes = buf.readableBytes();
                System.out.printf("SERVER STATE: Receiving content, readable bytes: %d%n", readableBytes);
                if (partLength > 0) {
                    int capacity = Math.min(readableBytes, (int)partLength);
                    byte[] currentPart = new byte[capacity];
                    buf.readBytes(currentPart);
                    System.arraycopy(currentPart, 0, currentData, currentPosition, capacity);
                    currentPosition += capacity;
                    partLength -= capacity;
                    if (partLength == 0) {
                        String contentString = new String(currentData, StandardCharsets.UTF_8);
                        System.out.printf("Server received data: %n%s%n", contentString);
                        ObjectMapper objectMapper = new ObjectMapper()
                                .registerModule(new JavaTimeModule());
                        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
                        Object content = objectMapper.readValue(contentString, Object.class);
                        currentState = State.IDLE;
                        handleClientRequest(ctx, content);
                    }
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    currentPosition = 0;
                    partLength = buf.readLong();
                    FileDescription fd = curUploadRequest.getFileDescription();
                    if (fd.isDirectory()) {
                        System.out.println("SERVER STATE: Directory was created");
                        currentState = State.IDLE;
                        UploadResponse ur = new UploadResponse(curUploadRequest.getId(), FileStatus.SUCCESS, "", currentPosition, partLength);
                        sendToClient(ctx, OperationType.FILE_STATUS, CommonUtils.serializeToJSONUnsafe(ur), true);
                        curUploadRequest = null;
                    } else {
                        currentState = State.FILE;
                        System.out.printf("SERVER STATE: Content length is %d bytes%n", partLength);
                    }
                }
            }

            if (currentState == State.FILE) {
                boolean isDone = false;
                int readableBytes = buf.readableBytes();
                FileDescription fd = curUploadRequest.getFileDescription();
                if (!fd.isDirectory()) {
                    System.out.printf("SERVER STATE: Receiving file, part size: %d%n", readableBytes);
                    if (fos != null && bos != null) {
                        while (buf.readableBytes() > 0) {
                            bos.write(buf.readByte());
                            currentPosition++;
                            isDone = (partLength == currentPosition);
                            if (isDone) {
                                currentState = State.IDLE;
                                System.out.println("SERVER STATE: File received");
                                bos.close();
                                bos = null;
                                fos.close();
                                fos = null;
                                break;
                            }
                        }
                        UploadResponse ur = new UploadResponse(
                                curUploadRequest.getId(),
                                (isDone ? FileStatus.SUCCESS : FileStatus.PROCESSING),
                                "", currentPosition, partLength);
                        sendToClient(ctx, OperationType.FILE_STATUS, CommonUtils.serializeToJSONUnsafe(ur), true);
                    }
                }
                if (isDone) curUploadRequest = null;
            }
        }
        if (buf.readableBytes() == 0) buf.release();
    }

    private void handleClientRequest(ChannelHandlerContext ctx, Object content) throws Exception {
        if (content instanceof AuthorizationRequest) handleAuthorizationRequest(ctx, (AuthorizationRequest) content);
        else if (content instanceof FileListRequest) handleFileListRequest(ctx, (FileListRequest) content);
        else if (content instanceof UploadRequest) handleUploadRequest(ctx, (UploadRequest) content);
        else if (content instanceof DirectoryRequest) handleDirectoryRequest(ctx, (DirectoryRequest) content);
        else if (content instanceof DownloadRequest) handleDownloadRequest(ctx, (DownloadRequest) content);
        else throw new Exception(String.format("Couldn't handle object: %s", content.getClass().toString()));
    }

    private void handleAuthorizationRequest(ChannelHandlerContext ctx, AuthorizationRequest commandRequest) throws Exception {
        String login = commandRequest.getLogin();
        UserManager um = server.getUserManager();
        OperationResult<Boolean> userCheckResult = um.userExists(commandRequest.getLogin());

        boolean newUser = false;
        boolean userExists = false;
        boolean passwordIsCorrect = false;
        AuthorizationResponse authResponse = null;

        if (userCheckResult.isSuccess()) {
            userExists = userCheckResult.getProduct();
            if (!userExists) {
                OperationResult<Boolean> registrationResult = um.addUser(commandRequest.getLogin(), commandRequest.getPassword());
                if (!registrationResult.isSuccess()) throw new RuntimeException(registrationResult.getDetailedMessage());
                newUser = true;
                userExists = true;
                passwordIsCorrect = true;
            }
        } else authResponse = new AuthorizationResponse(false, userCheckResult.getShortMessage());

        if (userExists && !newUser) {
            OperationResult<Boolean> passwordCheckResult = um.isPasswordCorrect(commandRequest.getLogin(), commandRequest.getPassword());
            if (passwordCheckResult.isSuccess()) {
                passwordIsCorrect = passwordCheckResult.getProduct();
                if (!passwordIsCorrect) authResponse = new AuthorizationResponse(false, "Password is incorrect");
            } else authResponse = new AuthorizationResponse(false, passwordCheckResult.getShortMessage());
        }
        boolean authorizationSuccessful = (userExists && passwordIsCorrect);
        if (authorizationSuccessful) {
            userFilesRoot = Paths.get(serverSettings.getStoragePath(), login);
            if (!(Files.exists(userFilesRoot))) Files.createDirectories(userFilesRoot);
            authResponse = new AuthorizationResponse(true, "", userFilesRoot.toString());
        }

        String authJSON = CommonUtils.serializeToJSONUnsafe(authResponse);
        sendToClient(ctx, OperationType.AUTHORIZATION, authJSON, true);
    }

    private void handleFileListRequest(ChannelHandlerContext ctx, FileListRequest commandRequest) throws Exception {
        String absolutePath = commandRequest.getAbsolutePath();
        Path curPath = null;
        if (absolutePath == null || absolutePath.isEmpty()) curPath = userFilesRoot;
        else {
            curPath = Paths.get(absolutePath);
            if (!Files.exists(curPath)) curPath = userFilesRoot;
        }
        ArrayList<FileDescription> fileList = new ArrayList<>();
        fillFileList(curPath, fileList, 1, false);
        String commandJSON = CommonUtils.serializeToJSONUnsafe(new FileListResponse(fileList));
        sendToClient(ctx, OperationType.FILE_LIST_REQUEST, commandJSON, true);
    }

    private void handleUploadRequest(ChannelHandlerContext ctx, UploadRequest commandRequest) throws Exception {
        curUploadRequest = commandRequest;
        destFilePath = userFilesRoot.resolve(curUploadRequest.getDestFileName());
        FileDescription fd = curUploadRequest.getFileDescription();
        if (fd.isDirectory()) {
            if (Files.notExists(destFilePath)) Files.createDirectories(destFilePath);
        }
        else {
            fos = new FileOutputStream(destFilePath.toString());
            bos = new BufferedOutputStream(fos);
        }
        currentState = State.FILE_LENGTH;
    }

    private void handleDirectoryRequest(ChannelHandlerContext ctx, DirectoryRequest commandRequest) throws Exception {
        ArrayList<FileDescription> requestList = commandRequest.getFilesList();
        ArrayList<FileDescription> responseList = new ArrayList<>(16);
        for (FileDescription fd: requestList) {
            final Path currentPath = Paths.get(fd.getAbsolutePath());
            fillFileList(currentPath, responseList, Integer.MAX_VALUE, true);
        }
        String commandJSON = CommonUtils.serializeToJSONUnsafe(new DirectoryResponse(responseList));
        sendToClient(ctx, OperationType.FILE_LIST_REQUEST, commandJSON, true);
    }

    private void handleDownloadRequest(ChannelHandlerContext ctx, DownloadRequest commandRequest) throws Exception {
        FileDescription fd = commandRequest.getFileDescription();
        Path file = Paths.get(fd.getAbsolutePath());
        if (Files.exists(file) && (fd.isDirectory() == Files.isDirectory(file))) {
            String command = CommonUtils.serializeToJSONUnsafe(new DownloadResponse(commandRequest));
            sendToClient(ctx, OperationType.DOWNLOAD_FILE, command, false);
            long fileLength = Files.size(file);

            ByteBuf data = Unpooled.directBuffer(8);
            data.writeLong(fileLength);
            ctx.writeAndFlush(data);

            if (!Files.isDirectory(file)) {
                FileRegion fileRegion = new DefaultFileRegion(file.toFile(), 0, fileLength);
                ctx.writeAndFlush(fileRegion);
            }
        } else throw new RuntimeException("Incorrect file request: " + fd.getAbsolutePath());
    }

    private void fillFileList(Path currentPath, ArrayList<FileDescription> fileList, int maxDepth, boolean hideParentDir) throws Exception {
        if (fileList == null) fileList = new ArrayList<>();
        List<Path> directories = null;
        try (Stream<Path> stream = Files.walk(currentPath, maxDepth)) {
            directories = stream
                    .filter(file -> Files.isDirectory(file))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw e;
        }
        if (directories != null) {
            for (Path dir : directories) {
                FileDescription fileDesc = null;
                if (!userFilesRoot.equals(dir)) {
                    if (hideParentDir) fileDesc = new FileDescription(dir);
                    else {
                        if (currentPath.equals(dir)) {
                            Path parentDir = dir.getParent();
                            fileDesc = new FileDescription(parentDir);
                            fileDesc.setName("..");
                        } else fileDesc = new FileDescription(dir);
                    }
                }
                if (fileDesc != null) fileList.add(fileDesc);
            }
        }
        List<Path> files = null;
        try (Stream<Path> stream = Files.walk(currentPath, maxDepth)) {
            files = stream
                    .filter(file -> !Files.isDirectory(file))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw e;
        }
        if (files != null) {
            for (Path file : files) {
                fileList.add(new FileDescription(file));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        sendToClient(ctx, OperationType.ERROR,
                CommonUtils.serializeToJSONUnsafe(ServerError.getExceptionResult(cause, true)), true);
        curUploadRequest = null;
        if (bos != null) bos.close();
        if (fos != null) fos.close();
        ctx.close();
    }

    private void sendToClient(ChannelHandlerContext ctx, OperationType operationType, String commandJSON, boolean finishTransfer) throws Exception {
        ByteBuf data = Unpooled.directBuffer(1);
        data.writeByte(operationType.getSignalByte());
        ctx.write(data);

        byte[] authJSONBytes = commandJSON.getBytes(StandardCharsets.UTF_8);
        data = Unpooled.directBuffer(8);
        data.writeLong(authJSONBytes.length);
        ctx.write(data);

        data = Unpooled.directBuffer(authJSONBytes.length);
        data.writeBytes(authJSONBytes);
        if (finishTransfer) ctx.writeAndFlush(data).sync();
        else ctx.writeAndFlush(data);
    }

}