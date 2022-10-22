package github.kasuminova.hyperserver.remoteserver;

import com.alibaba.fastjson2.JSON;
import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleDirectoryObject;
import github.kasuminova.balloonserver.utils.fileobject.SimpleFileObject;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.messages.StringMessage;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.ArrayList;

public class Methods {
    public static void sendFileList(ChannelHandlerContext ctx, String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            if (ctx != null) {
                ctx.writeAndFlush(new SimpleDirectoryObject(dir.getName(), new ArrayList<>()));
            }
            return;
        }

        File[] files = dir.listFiles();

        if (files == null) {
            if (ctx != null) {
                ctx.writeAndFlush(new SimpleDirectoryObject(dir.getName(), new ArrayList<>()));
            }
            return;
        }

        ArrayList<AbstractSimpleFileObject> fileObjectList = new ArrayList<>();
        ArrayList<AbstractSimpleFileObject> directoryObjectList = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) {
                fileObjectList.add(new SimpleFileObject(file.getName(), file.length(), "", file.lastModified()));
            } else {
                directoryObjectList.add(
                        new SimpleDirectoryObject(file.getName(), getDirectoryObjectList(path + "/" + file.getName())));
            }
        }

        directoryObjectList.addAll(fileObjectList);

        if (ctx != null) {
            ctx.writeAndFlush(new SimpleDirectoryObject(dir.getName(), directoryObjectList));
        }
    }

    public static void updateIntegratedServerConfig(ChannelHandlerContext ctx, String configJson) {
        IntegratedServerConfig config = JSON.parseObject(configJson, IntegratedServerConfig.class);

        ctx.writeAndFlush(new StringMessage("已更新远程服务器配置文件."));
    }

    private static ArrayList<AbstractSimpleFileObject> getDirectoryObjectList(String path) {
        File dir = new File(path);
        ArrayList<AbstractSimpleFileObject> fileObjectList = new ArrayList<>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileObjectList.add(new SimpleFileObject(file.getName(), file.length(), "", file.lastModified()));
                    } else {
                        fileObjectList.add(new SimpleDirectoryObject(file.getName(), new ArrayList<>()));
                    }
                }
            }
        }

        return fileObjectList;
    }
}
