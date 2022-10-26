package github.kasuminova.hyperserver.httpserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;

import static github.kasuminova.hyperserver.HyperServer.logger;

public class HttpServer {
    EventLoopGroup boss;
    EventLoopGroup work;
    ChannelFuture future;


    public void start() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        boss = new NioEventLoopGroup();
        work = new NioEventLoopGroup();

        HttpServerInitializer httpServerInitializer = new HttpServerInitializer();
        bootstrap.group(boss, work)
                .handler(new LoggingHandler(LogLevel.INFO))
                .channel(NioServerSocketChannel .class)
                .childHandler(httpServerInitializer);

        try {
            future = bootstrap.bind(new InetSocketAddress("0.0.0.0", 8080)).sync();
            logger.info("API 服务端已启动. IP: 0.0.0.0 端口: 8080");
        } catch (Exception e) {
            logger.error("无法启动服务器", e);
        }
    }
}
