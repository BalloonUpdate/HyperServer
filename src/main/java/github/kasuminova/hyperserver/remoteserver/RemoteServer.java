package github.kasuminova.hyperserver.remoteserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

import static github.kasuminova.hyperserver.HyperServer.logger;

public class RemoteServer {
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture future;

    public boolean start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup();
        work = new NioEventLoopGroup();

        RemoteServerInitializer remoteServerInitializer = new RemoteServerInitializer();
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel.class)
                .childHandler(remoteServerInitializer);

        try {
            future = bootstrap.bind(new InetSocketAddress("0.0.0.0", 20000)).sync();
            logger.info("远控服务端已启动. IP: 0.0.0.0 端口: 20000");
        } catch (InterruptedException e) {
            logger.error(e);
            return false;
        }

        return true;
    }

    public void stop() {
        work.shutdownGracefully();
        boss.shutdownGracefully();
        future.channel().close();
    }
}
