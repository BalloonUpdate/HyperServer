package github.kasuminova.hyperserver.remoteserver;

import github.kasuminova.messages.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

import static github.kasuminova.hyperserver.HyperServer.*;

public class RemoteChannel extends SimpleChannelInboundHandler<AbstractMessage> {
    boolean isAuthenticated = false;
    String clientIP;
    @Override
    public void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) {
        //认证
        if (!isAuthenticated && msg instanceof TokenMessage tokenMsg) {
            isAuthenticated = auth(ctx, tokenMsg);
        }

        if (isAuthenticated) {
            if (msg instanceof StringMessage strMsg) {
                logger.info("从客户端接收到消息: {}", strMsg.getMessage());
            } else if (msg instanceof MethodMessage rMsg) {
                MethodMessageProcessor.process(ctx, rMsg);
            } else {
                logger.warn("从客户端接收到未知消息类型: {}", msg.getMessageType());
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        clientIP = getClientIP(ctx);
        logger.info("{} 正在连接至服务器...", clientIP);
        connectedClientChannels.put(clientIP, ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("{} 已断开连接.", getClientIP(ctx));
        connectedClientChannels.remove(clientIP);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("{} 出现问题, 正在断开连接...\n{}", getClientIP(ctx), cause);
        connectedClientChannels.remove(clientIP);
        ctx.close();
    }

    public static String getClientIP(ChannelHandlerContext ctx) {
        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
        return socket.getAddress().getHostAddress();
    }

    private static boolean auth(ChannelHandlerContext ctx, TokenMessage tokenMsg) {
        logger.info("{} 正在认证... 客户端版本: {}", getClientIP(ctx), tokenMsg.getClientVersion());
        if (tokenMsg.getToken().equals(CONFIG.getToken())) {
            ctx.writeAndFlush(new StringMessage("认证成功."));
            logger.info("{} 认证成功.", getClientIP(ctx));
            return true;
        } else {
            ctx.writeAndFlush(new ErrorMessage("Token 错误."));
            logger.info("{} 认证错误, 断开连接.", getClientIP(ctx));
            ctx.close();
            return false;
        }
    }
}
