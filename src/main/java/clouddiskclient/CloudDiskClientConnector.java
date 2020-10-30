package clouddiskclient;

import common.OperationResult;
import common.SignalByte;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class CloudDiskClientConnector implements Runnable {

    private CloudDiskClient client;
    private ExecutorService executor = null;
    private boolean isRunning = false;
    private String serverHost;
    private int serverPort;
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

    public CloudDiskClientConnector(CloudDiskClient client, String serverHost, int serverPort) {
        this.client = client;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.executor = Executors.newFixedThreadPool(1);
    }

    public synchronized OperationResult startClient() {
        if (isRunning) return OperationResult.getSuccess("Client is already started");
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap conSettings = new Bootstrap();
            conSettings.group(workerGroup);
            conSettings.channel(NioSocketChannel.class);
            conSettings.remoteAddress(serverHost, serverPort);
            conSettings.option(ChannelOption.SO_KEEPALIVE, true);
            CloudDiskClientHandler handlerInstance = new CloudDiskClientHandler(client);
            conSettings.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(handlerInstance);
                    channel = socketChannel;
                }
            });
            connectionFuture = conSettings.connect().sync();
            executor.execute(this);
        } catch (Exception e) {
            stopClient(false);
            return OperationResult.getExceptionResult(e);
        }
        return OperationResult.getSuccess();
    }

    public synchronized OperationResult stopClient(boolean checkIfRunning) {
        if (checkIfRunning && !isRunning) return OperationResult.getSuccess("Client is already stopped");
        if (workerGroup != null) {
            try {
                workerGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                return OperationResult.getExceptionResult(e);
            }
            isRunning = false;
        }
        if (!executor.isShutdown()) {
            try {
                executor.shutdownNow();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) return OperationResult.getFailure("Couldn't terminate executor");
            } catch (InterruptedException e) {
                return OperationResult.getExceptionResult(e);
            }
        }
        return OperationResult.getSuccess();
    }

    @Override
    public void run() {
        try {
            connectionFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            isRunning = false;
        } finally {
            stopClient(false);
        }
    }

    public void authorizeUser(String login, String passwordHash, ChannelFutureListener finishListener) {
        ByteBuf content = Unpooled.directBuffer(1);
        content.writeByte(SignalByte.AUTHORIZATION.getValue());
        channel.write(content);

        byte[] loginBytes = login.getBytes(StandardCharsets.UTF_8);
        content = Unpooled.directBuffer(4);
        content.writeInt(loginBytes.length);
        channel.write(content);

        content = Unpooled.directBuffer(loginBytes.length);
        content.writeBytes(loginBytes);
        channel.write(content);

        byte[] passwordBytes = passwordHash.getBytes(StandardCharsets.UTF_8);
        content = Unpooled.directBuffer(4);
        content.writeInt(passwordBytes.length);
        channel.write(content);

        content = Unpooled.directBuffer(passwordBytes.length);
        content.writeBytes(passwordBytes);
        try {
            ChannelFuture transferOperationFuture = channel.writeAndFlush(content).sync();
            if (finishListener != null) transferOperationFuture.addListener(finishListener);
        } catch (InterruptedException e) {
            System.out.println("Algo esta yendo mal, puta madre!");
        }
    }

}