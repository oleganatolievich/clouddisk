import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Connector implements Runnable {

    private final Client client;
    private ExecutorService executor = null;
    private boolean isRunning = false;
    private SocketChannel channel;
    private ChannelFuture connectionFuture;
    private EventLoopGroup workerGroup;

    public boolean isRunning() {
        return isRunning;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public Connector(Client client) {
        this.client = client;
    }

    public synchronized OperationResult startClient() {
        if (isRunning) return OperationResult.getSuccess("Client is already started");
        executor = Executors.newFixedThreadPool(1);
        workerGroup = new NioEventLoopGroup();
        Settings clientSettings = client.getSettings();
        try {
            Bootstrap conSettings = new Bootstrap();
            conSettings.group(workerGroup);
            conSettings.channel(NioSocketChannel.class);
            conSettings.remoteAddress(clientSettings.getServerHost(), clientSettings.getServerPort());
            conSettings.option(ChannelOption.SO_KEEPALIVE, true);
            ConnectorHandler handlerInstance = new ConnectorHandler(client);
            conSettings.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(handlerInstance);
                    channel = socketChannel;
                }
            });
            isRunning = true;
            connectionFuture = conSettings.connect().sync();
            executor.execute(this);
        } catch (Exception e) {
            stopClient();
            return OperationResult.getExceptionResult(e);
        }
        return OperationResult.getSuccess(null);
    }

    public synchronized OperationResult stopClient() {
        isRunning = false;
        if (workerGroup != null) {
            try {
                workerGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                return OperationResult.getExceptionResult(e);
            }
        }
        if (!executor.isShutdown()) {
            try {
                executor.shutdownNow();
                if (!executor.awaitTermination(100, TimeUnit.MILLISECONDS)) return OperationResult.getFailure("Couldn't terminate executor");
            } catch (InterruptedException e) {
                return OperationResult.getExceptionResult(e);
            }
        }
        return OperationResult.getSuccess(null);
    }

    public synchronized void stopClientNow() {
        isRunning = false;
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (!executor.isShutdown())  executor.shutdownNow();
    }

    @Override
    public void run() {
        try {
            connectionFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            isRunning = false;
        } finally {
            stopClient();
        }
    }

    public OperationResult authorizeUser(String login, String password, ChannelFutureListener finishListener) {
        OperationResult funcResult = OperationResult.getSuccess(null);
        OperationResult<String> serializationResult = CommonUtils.serializeToJSON(new AuthorizationRequest(login, password));
        String command = null;
        if (serializationResult.isSuccess()) command = serializationResult.getProduct();
        else funcResult = serializationResult;
        if (command != null) funcResult = sendStringToServer(OperationType.AUTHORIZATION, command, finishListener);
        return funcResult;
    }

    public OperationResult uploadFile(UploadRequest ur, ChannelFutureListener finishListener) {
        OperationResult funcResult = OperationResult.getSuccess(null);
        OperationResult<String> serializationResult = CommonUtils.serializeToJSON(ur);
        String command = null;
        if (serializationResult.isSuccess()) command = serializationResult.getProduct();
        else funcResult = serializationResult;
        if (command != null) {
            ByteBuf data = Unpooled.directBuffer(1);
            data.writeByte(OperationType.UPLOAD_FILE.getSignalByte());
            channel.write(data);

            byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
            data = Unpooled.directBuffer(8);
            data.writeLong(commandBytes.length);
            channel.write(data);

            data = Unpooled.directBuffer(commandBytes.length);
            data.writeBytes(commandBytes);
            channel.writeAndFlush(data);

            try {
                Path file = Paths.get(ur.getFileDescription().getAbsolutePath());
                long fileLength = Files.size(file);
                data = Unpooled.directBuffer(8);
                data.writeLong(fileLength);
                channel.writeAndFlush(data);
                if (!Files.isDirectory(file)) {
                    FileRegion fileRegion = new DefaultFileRegion(file.toFile(), 0, fileLength);
                    ChannelFuture transferOperationFuture = channel.writeAndFlush(fileRegion);
                    if (finishListener != null) transferOperationFuture.addListener(finishListener);
                }
                funcResult = OperationResult.getSuccess(null);
            } catch (IOException e) {
                funcResult = OperationResult.getExceptionResult(e);
            }
        }
        return funcResult;
    }

    public OperationResult downloadFile(DownloadRequest dr, ChannelFutureListener finishListener) {
        OperationResult funcResult = OperationResult.getSuccess(null);
        OperationResult<String> serializationResult = CommonUtils.serializeToJSON(dr);
        String commandJSON = null;
        if (serializationResult.isSuccess()) commandJSON = serializationResult.getProduct();
        else funcResult = serializationResult;
        if (commandJSON != null) funcResult = sendStringToServer(OperationType.DOWNLOAD_FILE, commandJSON, finishListener);
        return funcResult;
    }

    public OperationResult getServerFilesList(String serverPath, ChannelFutureListener finishListener) {
        OperationResult funcResult = OperationResult.getSuccess(null);
        OperationResult<String> serializationResult = CommonUtils.serializeToJSON(new FileListRequest(serverPath));
        String commandJSON = null;
        if (serializationResult.isSuccess()) commandJSON = serializationResult.getProduct();
        else funcResult = serializationResult;
        if (commandJSON != null) funcResult = sendStringToServer(OperationType.FILE_LIST_REQUEST, commandJSON, finishListener);
        return funcResult;
    }

    public OperationResult requestFilesAtServer(ArrayList<FileDescription> fdList, ChannelFutureListener finishListener) {
        OperationResult funcResult = OperationResult.getSuccess(null);
        OperationResult<String> serializationResult = CommonUtils.serializeToJSON(new DirectoryRequest(fdList));
        String commandJSON = null;
        if (serializationResult.isSuccess()) commandJSON = serializationResult.getProduct();
        else funcResult = serializationResult;
        if (commandJSON != null) funcResult = sendStringToServer(OperationType.FILE_LIST_REQUEST, commandJSON, finishListener);
        return funcResult;
    }

    private OperationResult sendStringToServer(OperationType operationType, String command, ChannelFutureListener finishListener) {
        OperationResult funcResult;
        ByteBuf data = Unpooled.directBuffer(1);
        data.writeByte(operationType.getSignalByte());
        channel.write(data);

        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
        data = Unpooled.directBuffer(8);
        data.writeLong(commandBytes.length);
        channel.write(data);

        data = Unpooled.directBuffer(commandBytes.length);
        data.writeBytes(commandBytes);

        try {
            ChannelFuture transferOperationFuture = channel.writeAndFlush(data).sync();
            if (finishListener != null) transferOperationFuture.addListener(finishListener);
            funcResult = OperationResult.getSuccess(null);
        } catch (InterruptedException e) {
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

}