package clouddiskserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CloudDiskServer {

    private static Logger logger = LogManager.getLogger();
    private int portNumber;

    public CloudDiskServer(int portNumber) {
        this.portNumber = portNumber;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public static void main(String[] args) throws InterruptedException {
        CloudDiskServer server = new CloudDiskServer(49152);
        if (logger.isInfoEnabled()) logger.info("Starting server at port {}", server.getPortNumber());
        server.run();
    }

    public void run() throws InterruptedException {
        EventLoopGroup serverGroup = new NioEventLoopGroup();
        EventLoopGroup childrenGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverGroup, childrenGroup)
                .channel(NioServerSocketChannel.class) // TCP server socket
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(
                                new CloudDiskServerHandler());
                    }
                });

        ChannelFuture f = bootstrap.bind(portNumber).sync();
        f.channel().closeFuture().sync(); // block until the server channel is closed
    }

}