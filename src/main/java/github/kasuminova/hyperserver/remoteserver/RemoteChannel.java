package github.kasuminova.hyperserver.remoteserver;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.hyperserver.utils.RandomString;
import github.kasuminova.messages.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;

import static github.kasuminova.hyperserver.HyperServer.*;

public class RemoteChannel extends SimpleChannelInboundHandler<AbstractMessage> {
    boolean isAuthenticated = false;
    String clientIP;
    String clientID;
    @Override
    public void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) {
        //认证
        if (msg instanceof TokenMessage tokenMsg) {
            if (isAuthenticated) return;
            isAuthenticated = auth(ctx, clientIP, clientID, tokenMsg);
            connectedClientChannels.put(clientID, ctx);
        }

        if (isAuthenticated) {
            if (msg instanceof StringMessage strMsg) {
                logger.info("从客户端接收到消息: {}", strMsg.getMessage());
            } else if (msg instanceof MethodMessage rMsg) {
                MessageProcessor.processMethodMessage(ctx, rMsg);
            } else {
                logger.warn("从客户端接收到未知消息类型: {}", msg.getMessageType());
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        clientIP = getClientIP(ctx);
        clientID = RandomString.nextString(8);

        connectedClientChannels.put(clientID, ctx);
        logger.info("{} 正在连接至服务器...", clientIP);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("{} 已断开连接.", getClientIP(ctx));
        connectedClientChannels.remove(clientID);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("{} 出现问题, 正在断开连接...\n{}", clientIP, cause);
        connectedClientChannels.remove(clientID);
        ctx.close();
    }

    public static String getClientIP(ChannelHandlerContext ctx) {
        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
        return socket.getAddress().getHostAddress();
    }

    private static boolean auth(ChannelHandlerContext ctx, String clientIP, String clientID ,TokenMessage tokenMsg) {
        logger.info("{} 正在认证... 客户端版本: {}", clientIP, tokenMsg.getClientVersion());

        if (tokenMsg.getToken().equals(CONFIG.getToken())) {
            ctx.writeAndFlush(new StringMessage(StrUtil.format("认证成功, 已分配客户端 ID: {}", clientID)));
            logger.info("{} 认证成功.", clientIP);
            return true;
        } else {
            ctx.writeAndFlush(new ErrorMessage("Token 错误."));
            logger.info("{} 认证错误, 断开连接.", clientIP);
            ctx.close();
            return false;
        }
    }
}
