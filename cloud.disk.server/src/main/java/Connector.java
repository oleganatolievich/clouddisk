import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Connector implements Runnable {

    private final Server server;
    private ExecutorService executor = null;
    private boolean isRunning = false;
    private SocketChannel channel;
    private ChannelFuture connectionFuture;
    private EventLoopGroup serverGroup;
    private EventLoopGroup clientsGroup;

    public boolean isRunning() {
        return isRunning;
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public Connector(Server server) {
        this.server = server;
    }

    public synchronized OperationResult startServer() {
        if (isRunning) return OperationResult.getSuccess("Server is already started");
        executor = Executors.newFixedThreadPool(1);
        serverGroup = new NioEventLoopGroup();
        clientsGroup = new NioEventLoopGroup();
        Settings serverSettings = server.getSettings();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(serverGroup, clientsGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new ConnectorHandler(server));
                        }
                    });

            isRunning = true;
            connectionFuture = bootstrap.bind(serverSettings.getServerPort()).sync();
            executor.execute(this);
        } catch (Exception e) {
            stopServer();
            return OperationResult.getExceptionResult(e);
        }
        return OperationResult.getSuccess(null);
    }

    public synchronized OperationResult stopServer() {
        if (serverGroup != null) {
            try {
                serverGroup.shutdownGracefully().sync();
                clientsGroup.shutdownGracefully().sync();
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
        return OperationResult.getSuccess(null);
    }

    public synchronized void stopServerNow() {
        isRunning = false;
        if (serverGroup != null) serverGroup.shutdownGracefully();
        if (clientsGroup != null) clientsGroup.shutdownGracefully();
        if (!executor.isShutdown())  executor.shutdownNow();
    }

    @Override
    public void run() {
        try {
            connectionFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            isRunning = false;
        } finally {
            stopServer();
        }
    }

}