import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConnectorHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE,
        CONTENT_LENGTH,
        CONTENT,
        FILE_LENGTH,
        FILE
    }

    private final Client client;
    private State currentState = State.IDLE;
    private OperationType currentOperation;
    private long partLength;
    private int currentPosition;
    private byte[] currentData;
    private Path destFilePath;
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private DownloadResponse curDownloadResponse;

    public ConnectorHandler(Client client) {
        this.client = client;
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
                    System.out.printf("CLIENT STATE: %s, waiting for content length%n", currentOperation.toString());
                } else System.out.printf("CLIENT ERROR: Couldn't find operation for signal byte: %d%n", readed);
            }

            if (currentState == State.CONTENT_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    currentPosition = 0;
                    partLength = buf.readLong();
                    currentData = new byte[(int)partLength];
                    currentState = State.CONTENT;
                    System.out.printf("CLIENT STATE: Content length is %d bytes%n", partLength);
                }
            }

            if (currentState == State.CONTENT) {
                int readableBytes = buf.readableBytes();
                if (partLength > 0) {
                    int capacity = Math.min(readableBytes, (int)partLength);
                    byte[] currentPart = new byte[capacity];
                    buf.readBytes(currentPart);
                    System.arraycopy(currentPart, 0, currentData, currentPosition, capacity);
                    currentPosition += capacity;
                    partLength -= capacity;
                    if (partLength == 0) {
                        String contentString = new String(currentData, StandardCharsets.UTF_8);
                        System.out.printf("Client received data: %n%s%n", contentString);
                        ObjectMapper objectMapper = new ObjectMapper()
                                .registerModule(new JavaTimeModule());
                        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(),
                                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);
                        Object content = objectMapper.readValue(contentString, Object.class);
                        currentState = State.IDLE;
                        handleServerResponse(ctx, content);
                    }
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    currentPosition = 0;
                    partLength = buf.readLong();
                    FileDescription fd = curDownloadResponse.getFileDescription();
                    if (fd.isDirectory()) {
                        System.out.println("CLIENT STATE: Directory was created");
                        currentState = State.IDLE;
                        client.updateDownloadStatus(curDownloadResponse, FileStatus.SUCCESS);
                        curDownloadResponse = null;
                    } else {
                        currentState = State.FILE;
                        System.out.printf("CLIENT STATE: Content length is %d bytes%n", partLength);
                    }
                }
            }

            if (currentState == State.FILE) {
                boolean isDone = false;
                int readableBytes = buf.readableBytes();
                FileDescription fd = curDownloadResponse.getFileDescription();
                if (!fd.isDirectory()) {
                    System.out.printf("CLIENT STATE: Receiving file, part size: %d%n", readableBytes);
                    if (fos != null && bos != null) {
                        while (buf.readableBytes() > 0) {
                            bos.write(buf.readByte());
                            currentPosition++;
                            isDone = (partLength == currentPosition);
                            if (isDone) {
                                currentState = State.IDLE;
                                System.out.println("CLIENT STATE: File received");
                                bos.close();
                                bos = null;
                                fos.close();
                                fos = null;
                                break;
                            }
                        }
                        client.updateDownloadStatus(curDownloadResponse, (isDone ? FileStatus.SUCCESS : FileStatus.PROCESSING));
                    }
                }
                if (isDone) curDownloadResponse = null;
            }
        }
        if (buf.readableBytes() == 0) buf.release();
    }

    private void handleServerResponse(ChannelHandlerContext ctx, Object content) throws Exception {
        if (content instanceof AuthorizationResponse) handleAuthorizationResponse(ctx, (AuthorizationResponse) content);
        else if (content instanceof FileListResponse) handleFileListResponse(ctx, (FileListResponse) content);
        else if (content instanceof UploadResponse) handleUploadResponse(ctx, (UploadResponse) content);
        else if (content instanceof DirectoryResponse) handleDirectoryResponse(ctx, (DirectoryResponse) content);
        else if (content instanceof DownloadResponse) handleDownloadResponse(ctx, (DownloadResponse) content);
        else throw new Exception(String.format("Couldn't handle object: %s", content.getClass().toString()));
    }

    private void handleAuthorizationResponse(ChannelHandlerContext ctx, AuthorizationResponse commandResponse) throws Exception {
        OperationResult authResult;
        if (commandResponse.isSuccess()) authResult = OperationResult.getSuccess(commandResponse);
        else authResult = OperationResult.getFailure(commandResponse.getMessage());
        client.handleAuthorizationResponse(authResult);
    }

    private void handleFileListResponse(ChannelHandlerContext ctx, FileListResponse commandResponse) throws Exception {
        client.handleFileListResponse(commandResponse);
    }

    private void handleDirectoryResponse(ChannelHandlerContext ctx, DirectoryResponse commandResponse) throws Exception {
        client.handleDirectoryResponse(commandResponse);
    }

    private void handleUploadResponse(ChannelHandlerContext ctx, UploadResponse commandResponse) throws Exception {
        client.handleUploadResponse(commandResponse);
    }

    private void handleDownloadResponse(ChannelHandlerContext ctx, DownloadResponse commandResponse) throws Exception {
        curDownloadResponse = commandResponse;
        destFilePath = Paths.get(curDownloadResponse.getDestFileName());
        FileDescription fd = curDownloadResponse.getFileDescription();
        if (fd.isDirectory()) {
            if (Files.notExists(destFilePath)) Files.createDirectories(destFilePath);
        }
        else {
            fos = new FileOutputStream(destFilePath.toString());
            bos = new BufferedOutputStream(fos);
        }
        currentState = State.FILE_LENGTH;
    }

    private void handleServerError(ChannelHandlerContext ctx, ServerError commandResponse) throws Exception {
        client.handleServerError(commandResponse);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    
}