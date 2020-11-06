import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

    private Settings settings;
    private final String settingsFileName = "server_settings.json";
    private static Logger logger = LogManager.getLogger();

    public Server() {
        settings = Settings.getDefaults();
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        if (logger.isInfoEnabled()) logger.info("Starting server at port {}", server.settings.getClientPort());
        server.run();
    }

    public void run() throws InterruptedException {
        EventLoopGroup serverGroup = new NioEventLoopGroup();
        EventLoopGroup childrenGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(serverGroup, childrenGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new ServerHandler());
                    }
                });

        ChannelFuture f = bootstrap.bind(settings.getClientPort()).sync();
        f.channel().closeFuture().sync();
    }

}