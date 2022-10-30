package github.kasuminova.messages.processor;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.hyperserver.HyperServer;
import github.kasuminova.hyperserver.remoteserver.Methods;
import github.kasuminova.hyperserver.utils.MiscUtils;
import github.kasuminova.hyperserver.utils.NetworkLogger;
import github.kasuminova.messages.LogMessage;
import github.kasuminova.messages.MethodMessage;
import github.kasuminova.messages.RequestMessage;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;

public class MessageProcessor {
    /**
     * 执行消息中的方法
     * @param ctx 如果出现错误, 向客户端发送消息
     * @param message 消息
     */
    public static void processMethodMessage(ChannelHandlerContext ctx, MethodMessage message) {
        try {
            //包名 + 类名
            Class<?> classZZ = Class.forName(message.getClassName());
            //获取参数
            String[] params = message.getParams();
            if (params != null && params.length > 0) {
                //传递需要执行的方法
                Method method = classZZ.getMethod(message.getMethodName(), String[].class);
                method.invoke(null, (Object) params);
            } else {
                //传递需要执行的方法
                Method method = classZZ.getMethod(message.getMethodName());
                method.invoke(null);
            }

            HyperServer.logger.debug("已执行操作: {}.{}", message.getClassName(), message.getMethodName());
        } catch (Exception e) {
            ctx.writeAndFlush(new LogMessage(NetworkLogger.ERROR,
                    StrUtil.format("执行函数 {}.{} 的时候出现了问题:\n{}",
                            message.getClassName(),
                            message.getMethodName(),
                            MiscUtils.stackTraceToString(e))
            ));
        }
    }

    /**
     * 处理 RequestMessage 内容
     * @param ctx 如果请求有返回值或出现问题, 则使用此通道发送消息
     * @param message 消息
     */
    public static void processRequestMessage(ChannelHandlerContext ctx, RequestMessage message) {
        String requestType = message.getRequestType();

        switch (requestType) {
            case "GetFileList" -> Methods.sendFileList(
                    ctx, message.getRequestParams().get(0));

            case "MemoryGC" -> System.gc();

            case "UpdateIntegratedServerConfig" -> Methods.updateIntegratedServerConfig(
                    ctx, message.getRequestParams().get(0));
        }
    }
}
