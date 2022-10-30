package github.kasuminova.hyperserver.remoteserver;

import cn.hutool.core.util.StrUtil;
import github.kasuminova.hyperserver.HyperServer;
import github.kasuminova.hyperserver.utils.MiscUtils;
import github.kasuminova.hyperserver.utils.NetworkLogger;
import github.kasuminova.messages.LogMessage;
import github.kasuminova.messages.filemessages.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public class RemoteFileChannel extends SimpleChannelInboundHandler<FileMessage> {
    private final Map<String, NetworkFile> networkFiles = new HashMap<>();
    private Timer fileDaemon;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FileMessage msg) {
        String path = StrUtil.format("{}/{}", msg.getFilePath(), msg.getFileName());
        File file = new File(path);

        if (msg instanceof FileRequestMsg requestMsg) {
            try {
                sendFile(ctx, path, file, requestMsg);
            } catch (IOException e) {
                ctx.writeAndFlush(new LogMessage(NetworkLogger.ERROR, MiscUtils.stackTraceToString(e)));
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    public void sendFile(ChannelHandlerContext ctx, String path, File file, FileRequestMsg requestMsg) throws IOException {
        NetworkFile networkFile = networkFiles.get(path);
        if (networkFile == null) {
            networkFile = new NetworkFile(
                    System.currentTimeMillis(),
                    new RandomAccessFile(file, "r")
            );
            networkFiles.put(path, networkFile);
        }

        int length = requestMsg.getLength() == -1 ? 8192 : (int) requestMsg.getLength();

        byte[] data = new byte[length];

        RandomAccessFile randomAccessFile = networkFile.randomAccessFile;

        randomAccessFile.seek(requestMsg.getOffset());
        randomAccessFile.read(data);

        ctx.writeAndFlush(new FileObjMessage(
                requestMsg.getFilePath(), requestMsg.getFileName(),
                requestMsg.getOffset(), length,
                randomAccessFile.length(), data));

        networkFile.lastUpdateTime = System.currentTimeMillis();
        networkFile.completedBytes += length;

        if (networkFile.completedBytes >= randomAccessFile.length()) {
            randomAccessFile.close();
            networkFiles.remove(path);
            HyperServer.logger.info("Successfully To Upload File {}", path);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        fileDaemon = new Timer(1000, e -> networkFiles.forEach((path, networkFile) -> {
            if (networkFile.lastUpdateTime < System.currentTimeMillis() - 5000) {
                try {
                    networkFile.randomAccessFile.close();
                    HyperServer.logger.warn("文件 {} 上传超时, 已停止占用本地文件.", path);
                } catch (Exception ex) {
                    HyperServer.logger.error(ex);
                }
                networkFiles.remove(path);
            }
        }));
        fileDaemon.start();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        networkFiles.forEach((path, networkFile) -> {
            try {
                networkFile.randomAccessFile.close();
            } catch (Exception e) {
                HyperServer.logger.error(e);
            }
            networkFiles.remove(path);
        });

        fileDaemon.stop();
    }

    private static class NetworkFile {
        private long lastUpdateTime;
        private final RandomAccessFile randomAccessFile;
        private long completedBytes = 0;

        public NetworkFile(long lastUpdateTime, RandomAccessFile file) {
            this.lastUpdateTime = lastUpdateTime;
            this.randomAccessFile = file;
        }
    }
}
