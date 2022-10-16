package github.kasuminova.messages;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.hyperserver.HyperServer;
import github.kasuminova.hyperserver.utils.MiscUtils;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;

public class MethodMessageProcessor {
    /**
     * 执行消息中的方法
     * @param ctx 如果出现错误, 向客户端发送消息
     * @param message 消息
     */
    public static void process(ChannelHandlerContext ctx, MethodMessage message) {
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
            ctx.writeAndFlush(new ErrorMessage(
                    StrUtil.format("执行目标函数的时候出现了问题 {}.{}",
                    message.getClassName(),
                    message.getMethodName()),
                    MiscUtils.stackTraceToString(e)
            ));
        }
    }
}
