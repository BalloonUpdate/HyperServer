package github.kasuminova.hyperserver.httpserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static github.kasuminova.hyperserver.HyperServer.logger;
import static github.kasuminova.hyperserver.utils.MiscUtils.formatTime;

public final class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        final String uri = req.uri();
        long start = System.currentTimeMillis();

        String clientIP = getClientIP(ctx, req);
        //转义后的 URI
        String decodedURI = URLDecoder.decode(uri, StandardCharsets.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer("Hello World!", StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        printSuccessLog(System.currentTimeMillis() - start, "200", clientIP, decodedURI);
    }

    /**
     * 打印 200 OK 日志
     *
     * @param usedTime   耗时
     * @param clientIP   客户端 IP
     * @param decodedURI 转义后的 URI
     */
    private static void printSuccessLog(long usedTime, String status, String clientIP, String decodedURI) {
        //格式为 IP, 额外信息, 转义后的 URI, 耗时
        logger.info(
                String.format("%s\t| %s | %s | %s",
                        clientIP,
                        status,
                        formatTime(usedTime),
                        decodedURI
                ));
    }

    /**
     * 获取客户端 IP
     */
    private static String getClientIP(ChannelHandlerContext ctx, FullHttpRequest req) {
        //获取客户端 IP
        Attribute<String> channelAttr = ctx.channel().attr(DecodeProxy.key);
        if (channelAttr.get() != null) {
            return channelAttr.get();
        } else {
            String clientIP = req.headers().get("X-Forwarded-For");
            if (clientIP == null) {
                InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
                clientIP = socket.getAddress().getHostAddress();
            }
            return clientIP;
        }
    }
}
