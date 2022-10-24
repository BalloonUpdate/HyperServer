package github.kasuminova.hyperserver.remoteserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class RemoteServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new ObjectEncoder());//编码器
        pipeline.addLast(new ObjectDecoder(ClassResolvers.weakCachingResolver(Object.class.getClassLoader())));//解码器
        pipeline.addLast(new RemoteChannel());
    }
}
