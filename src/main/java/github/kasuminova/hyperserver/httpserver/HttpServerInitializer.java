package github.kasuminova.hyperserver.httpserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast("proxy-decoder", new DecodeProxy());
        pipeline.addLast("http-codec", new HttpServerCodec());
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(65536));

        pipeline.addLast("http-handler", new HttpRequestHandler());
    }
}
