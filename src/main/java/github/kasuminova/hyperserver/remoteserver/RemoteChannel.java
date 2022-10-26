package github.kasuminova.hyperserver.remoteserver;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.hyperserver.utils.RandomString;
import github.kasuminova.messages.*;
import github.kasuminova.messages.filemessages.FileMessage;
import github.kasuminova.messages.processor.MessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;
import java.util.Arrays;

import static github.kasuminova.hyperserver.HyperServer.*;

public class RemoteChannel extends SimpleChannelInboundHandler<Object> {
    boolean isAuthenticated = false;
    String clientIP;
    String clientID;
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        //认证
        if (msg instanceof TokenMessage tokenMsg) {
            isAuthenticated = auth(ctx, clientIP, clientID, tokenMsg);
            return;
        }

        //请求信息
        if (msg instanceof RequestMessage requestMsg) {
            MessageProcessor.processRequestMessage(ctx, requestMsg);
            return;
        }

        //方法反射信息
        if (msg instanceof MethodMessage methodMsg) {
            MessageProcessor.processMethodMessage(ctx, methodMsg);
            return;
        }

        //文件传输信息
        if (msg instanceof FileMessage fileMsg) {
            ctx.fireChannelRead(fileMsg);
            return;
        }

        //普通信息
        if (msg instanceof StringMessage strMsg) {
            logger.info("从客户端接收到消息: {}", strMsg.getMessage());
            return;
        }

        logger.warn("从客户端接收到未知消息: {}", msg.toString());
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

    /**
     * 认证客户端
     *
     * @param ctx 通道
     * @param clientIP 客户端 IP
     * @param clientID 客户端 ID
     * @param tokenMsg token
     * @return 是否认证成功
     */
    private static boolean auth(ChannelHandlerContext ctx, String clientIP, String clientID ,TokenMessage tokenMsg) {
        logger.info("{} 正在认证... 客户端版本: {}", clientIP, tokenMsg.getClientVersion());

        if (isUnSupportedVersion(tokenMsg.getClientVersion())) {
            ctx.writeAndFlush(new ErrorMessage(StrUtil.format("不兼容的客户端版本. (支持的客户端版本: {})", Arrays.toString(supportedClientVersions))));
            logger.info("{} 客户端不兼容, 断开连接.", clientIP);
            ctx.close();
            return false;
        }

        if (tokenMsg.getToken().equals(CONFIG.getToken())) {
            ctx.writeAndFlush(new StringMessage(StrUtil.format("认证成功, 已分配客户端 ID: {}", clientID)));
            logger.info("{} 认证成功, 已分配客户端 ID: {}", clientIP, clientID);
            return true;
        } else {
            ctx.writeAndFlush(new ErrorMessage("Token 错误."));
            logger.info("{} 认证错误, 断开连接.", clientIP);
            ctx.close();
            return false;
        }
    }

    /**
     * 验证版本是否为不兼容的版本
     *
     * @param clientVersion 要验证的版本
     */
    private static boolean isUnSupportedVersion(ApplicationVersion clientVersion) {
        for (ApplicationVersion supportedVersion : supportedClientVersions) {
            if (clientVersion.toInt() == supportedVersion.toInt()) {
                return false;
            }
        }

        return true;
    }
}
