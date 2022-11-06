package github.kasuminova.hyperserver.remoteserver;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleFileObject;
import github.kasuminova.hyperserver.utils.NetworkLogger;
import github.kasuminova.hyperserver.utils.RandomString;
import github.kasuminova.messages.*;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static github.kasuminova.hyperserver.HyperServer.*;

public class RemoteChannel extends AbstractRemoteChannel {
    boolean isAuthenticated = false;

    @Override
    public boolean channelRead1(Object msg) {
        if (msg instanceof TokenMessage tokenMessage) {
            auth(tokenMessage);
            return false;
        }
        return isAuthenticated;
    }

    @Override
    public void onRegisterMessages() {
        registerMessage(TokenMessage.class, (MessageProcessor<TokenMessage>) this::auth);
        registerMessage(LogMessage.class, (MessageProcessor<LogMessage>) message0 -> logger.info("从客户端接收到消息: {}", message0.getMessage()));
        registerMessage(RequestMessage.class, (MessageProcessor<RequestMessage>) message0 -> processRequestMessage(ctx, message0));
    }

    @Override
    public void channelActive0() {
        clientID = RandomString.nextString(8);

        connectedClientChannels.put(clientID, ctx);
        logger.info("{} 正在连接至服务器...", clientIP);
    }

    @Override
    public void channelInactive0() {
        logger.info("{} 已断开连接.", clientIP);
        connectedClientChannels.remove(clientID);
    }

    @Override
    public void exceptionCaught0(Throwable cause) {
        logger.warn("{} 出现问题, 正在断开连接...\n{}", clientIP, cause);
        connectedClientChannels.remove(clientID);
    }

    /**
     * 认证客户端
     *
     * @param tokenMsg token
     */
    private void auth(TokenMessage tokenMsg) {
        logger.info("{} 正在认证... 客户端版本: {}", clientIP, tokenMsg.getClientVersion());

        if (isUnSupportedVersion(tokenMsg.getClientVersion())) {
            ctx.writeAndFlush(new LogMessage(NetworkLogger.ERROR, StrUtil.format("不兼容的客户端版本. (支持的客户端版本: {})", Arrays.toString(supportedClientVersions))));
            logger.info("{} 客户端不兼容, 断开连接.", clientIP);
            ctx.close();

            isAuthenticated = false;
            return;
        }

        if (tokenMsg.getToken().equals(HYPERSERVER_CONFIG.getToken())) {
            ctx.writeAndFlush(new LogMessage(NetworkLogger.INFO, StrUtil.format("认证成功, 已分配客户端 ID: {}", clientID)));
            ctx.writeAndFlush(new AuthSuccessMessage(clientID, HTTPSERVER_CONFIG));
            logger.info("{} 认证成功, 已分配客户端 ID: {}", clientIP, clientID);

            isAuthenticated = true;
        } else {
            ctx.writeAndFlush(new LogMessage(NetworkLogger.ERROR, "Token 错误."));
            logger.info("{} 认证错误, 断开连接.", clientIP);
            ctx.close();

            isAuthenticated = false;
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

    /**
     * 处理 RequestMessage 内容
     * @param ctx 如果请求有返回值或出现问题, 则使用此通道发送消息
     * @param message 消息
     */
    public static void processRequestMessage(ChannelHandlerContext ctx, RequestMessage message) {
        String requestType = message.getRequestType();

        switch (requestType) {
            case "GetFileList" -> sendFileList(ctx, message.getRequestParams().get(0));
            case "MemoryGC" -> System.gc();
            case "UpdateIntegratedServerConfig" -> updateIntegratedServerConfig(ctx, message.getRequestParams().get(0));
        }
    }

    public static void sendFileList(ChannelHandlerContext ctx, String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            ctx.writeAndFlush(new SimpleDirectoryObject(dir.getName(), new ArrayList<>()));
            return;
        }

        File[] files = dir.listFiles();

        if (files == null) {
            ctx.writeAndFlush(new SimpleDirectoryObject(dir.getName(), new ArrayList<>()));
            return;
        }

        ctx.writeAndFlush(new SimpleDirectoryObject(dir.getName(), getDirectoryObjectList(path)));
    }

    public static void updateIntegratedServerConfig(ChannelHandlerContext ctx, String configJson) {
        IntegratedServerConfig config = JSON.parseObject(configJson, IntegratedServerConfig.class);

        ctx.writeAndFlush(new LogMessage(NetworkLogger.INFO, "已更新远程服务器配置文件."));
    }

    private static ArrayList<AbstractSimpleFileObject> getDirectoryObjectList(String path) {
        File dir = new File(path);
        ArrayList<AbstractSimpleFileObject> fileObjectList = new ArrayList<>();
        ArrayList<AbstractSimpleFileObject> directoryObjectList = new ArrayList<>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileObjectList.add(new SimpleFileObject(file.getName(), file.length(), "", file.lastModified()));
                    } else {
                        directoryObjectList.add(new SimpleDirectoryObject(file.getName(), getDirectoryObjectList(path + "/" + file.getName())));
                    }
                }
            }
        }

        directoryObjectList.addAll(fileObjectList);

        return directoryObjectList;
    }
}
