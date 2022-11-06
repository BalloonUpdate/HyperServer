package github.kasuminova.hyperserver.remoteserver;

import github.kasuminova.hyperserver.HyperServer;
import github.kasuminova.messages.MessageProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static github.kasuminova.hyperserver.HyperServer.logger;

public abstract class AbstractRemoteChannel extends SimpleChannelInboundHandler<Object> {
    protected String clientIP;
    protected String clientID;

    protected final Map<Class<?>, MessageProcessor<?>> messageProcessors = new HashMap<>(4);
    protected ChannelHandlerContext ctx;

    @Override
    protected final void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (channelRead1(msg)) {
            MessageProcessor<Object> processor = (MessageProcessor<Object>) messageProcessors.get(msg.getClass());
            if (processor != null) {
                processor.process(msg);
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    @Override
    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        onRegisterMessages();

        super.channelRegistered(ctx);
    }

    @Override
    public final void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        clientIP = getClientIP(ctx);

        channelActive0();

        super.channelActive(ctx);
    }

    @Override
    public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelInactive0();

        super.channelInactive(ctx);
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        exceptionCaught0(cause);

        ctx.close();
    }

    /**
     * 开始注册消息事件
     */
    protected void onRegisterMessages() {

    }

    /**
     * 通道启用事件
     */
    protected void channelActive0() {

    }

    /**
     * 通道接收到消息时
     * @param msg 消息
     * @return 是否继续读取消息
     */
    protected boolean channelRead1(Object msg) {
        return true;
    }

    /**
     * 通道关闭事件
     */
    protected void channelInactive0() {

    }

    /**
     * 通道出现问题时
     * @param cause 错误
     */
    protected void exceptionCaught0(Throwable cause) {

    }

    /**
     * 注册消息以及对应的处理器
     * @param clazz 消息类型
     * @param processor 消息处理函数
     */
    public void registerMessage(Class<?> clazz, MessageProcessor<?> processor) {
        messageProcessors.put(clazz, processor);
        if (HyperServer.HYPERSERVER_CONFIG.isDebugMode()) {
            logger.debug("Registered Message {}", clazz.getName());
        }
    }

    public static String getClientIP(ChannelHandlerContext ctx) {
        InetSocketAddress socket = (InetSocketAddress) ctx.channel().remoteAddress();
        return socket.getAddress().getHostAddress();
    }
}
