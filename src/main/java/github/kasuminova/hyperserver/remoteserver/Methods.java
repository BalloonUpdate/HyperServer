package github.kasuminova.hyperserver.remoteserver;

import github.kasuminova.hyperserver.HyperServer;
import github.kasuminova.messages.fileobject.AbstractLiteFileObject;
import github.kasuminova.messages.fileobject.LiteDirectoryObject;
import github.kasuminova.messages.fileobject.LiteFileObject;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.ArrayList;

public class Methods {
    public static void sendFileList(String[] params) {
        if (params == null || params.length != 2) {
            return;
        }

        String clientIP = params[0];
        String path = params[1];

        ChannelHandlerContext ctx = HyperServer.connectedClientChannels.get(clientIP);

        if (path == null || path.isEmpty()) {
            if (ctx != null) {
                ctx.writeAndFlush(new LiteDirectoryObject("null", new ArrayList<>()));
            }
            return;
        }

        File dir = new File(path);

        if (!dir.exists()) {
            if (ctx != null) {
                ctx.writeAndFlush(new LiteDirectoryObject(dir.getName(), new ArrayList<>()));
            }
            return;
        }

        File[] files = dir.listFiles();

        if (files == null) {
            if (ctx != null) {
                ctx.writeAndFlush(new LiteDirectoryObject(dir.getName(), new ArrayList<>()));
            }
            return;
        }

        ArrayList<AbstractLiteFileObject> fileObjectList = new ArrayList<>();
        ArrayList<AbstractLiteFileObject> directoryObjectList = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) {
                fileObjectList.add(new LiteFileObject(file.getName(), file.length(), file.lastModified()));
            } else {
                directoryObjectList.add(
                        new LiteDirectoryObject(file.getName(), getDirectoryObjectList(path + "/" + file.getName())));
            }
        }

        directoryObjectList.addAll(fileObjectList);

        if (ctx != null) {
            ctx.writeAndFlush(new LiteDirectoryObject(dir.getName(), directoryObjectList));
        }
    }

    private static ArrayList<AbstractLiteFileObject> getDirectoryObjectList(String path) {
        File dir = new File(path);
        ArrayList<AbstractLiteFileObject> fileObjectList = new ArrayList<>();
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileObjectList.add(new LiteFileObject(file.getName(), file.length(), file.lastModified()));
                    } else {
                        fileObjectList.add(new LiteDirectoryObject(file.getName(), new ArrayList<>()));
                    }
                }
            }
        }

        return fileObjectList;
    }
}
