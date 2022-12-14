package github.kasuminova.balloonserver.utils.filecacheutils;

import cn.hutool.core.thread.ThreadUtil;
import github.kasuminova.balloonserver.utils.fileobject.*;
import github.kasuminova.hyperserver.utils.NetworkLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 计算资源缓存的公用类
 */
public class FileCacheCalculator {
    private final AtomicLong completedBytes = new AtomicLong(0);
    private final AtomicInteger completedFiles = new AtomicInteger(0);
    private final String hashAlgorithm;
    //线程池
    private final ThreadPoolExecutor fileThreadPool = new ThreadPoolExecutor(
            0, Runtime.getRuntime().availableProcessors() * 2,
            0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    public FileCacheCalculator(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    /**
     * 扫描目标文件夹内的文件与文件夹
     *
     * @param directory 目标文件夹
     * @param logger    日志输出器
     * @return ArrayList<AbstractSimpleFileObject>, 如果文件夹内容为空则返回空 ArrayList
     */
    public ArrayList<AbstractSimpleFileObject> scanDir(File directory, NetworkLogger logger) {
        File[] fileList = directory.listFiles();
        if (fileList == null) {
            return new ArrayList<>();
        }
        ArrayList<FutureTask<SimpleFileObject>> fileCounterTaskList = new ArrayList<>();
        ArrayList<FutureTask<SimpleDirectoryObject>> direCounterTaskList = new ArrayList<>();
        ArrayList<AbstractSimpleFileObject> abstractSimpleFileObjectList = new ArrayList<>();

        for (File file : fileList) {
            if (file.isFile()) {
                FutureTask<SimpleFileObject> fileInfoTask = new FutureTask<>(new FileInfoTask(file, hashAlgorithm, completedBytes, completedFiles));
                fileCounterTaskList.add(fileInfoTask);
                fileThreadPool.submit(fileInfoTask);
            } else {
                FutureTask<SimpleDirectoryObject> dirCounterTask = new FutureTask<>(new DirInfoTask(file, hashAlgorithm, completedBytes, completedFiles));
                direCounterTaskList.add(dirCounterTask);
                ThreadUtil.execute(dirCounterTask);
            }
        }

        for (FutureTask<SimpleDirectoryObject> simpleDirectoryObjectFutureTask : direCounterTaskList) {
            try {
                abstractSimpleFileObjectList.add(simpleDirectoryObjectFutureTask.get());
            } catch (Exception ignored) {}
        }

        for (FutureTask<SimpleFileObject> simpleFileObjectFutureTask : fileCounterTaskList) {
            try {
                abstractSimpleFileObjectList.add(simpleFileObjectFutureTask.get());
            } catch (Exception ignored) {}
        }

        //回收线程池
        ThreadUtil.execute(() -> {
            fileThreadPool.shutdownNow();
            logger.info("已回收所有闲置线程.");
        });

        return abstractSimpleFileObjectList;
    }

    /**
     * 计算文件夹内容大小
     */
    private static class FileCounter {
        private final AtomicLong totalSize = new AtomicLong(0);
        private final AtomicLong totalFiles = new AtomicLong();

        private long[] getFiles(File dir) {
//            statusProgressBar.setString("扫描文件夹内容... (0 Byte, 0 文件)");
//            Timer timer = new Timer(250, e -> statusProgressBar.setString(
//                    String.format("扫描文件夹内容... (%s, %s 文件)",
//                            FileUtil.formatFileSizeToStr(totalSize.get()),
//                            totalFiles.get())));
//            timer.start();
//
//            statusProgressBar.setVisible(true);
//            statusProgressBar.setIndeterminate(true);
//
//            try {
//                new DirSizeCalculatorThread(dir, totalSize, totalFiles).run();
//                timer.stop();
//            } catch (Exception e) {
//                e.printStackTrace();
//                return new long[]{0, 0};
//            }
//            return new long[]{totalSize.get(), totalFiles.get()};
            return null;
        }
    }

    /**
     * <p>
     * 统计目标文件夹内包含的 文件/文件夹 大小.
     * </p>
     *
     * <p>
     * 并将其大小整合在一起至一个变量, 用于轮询线程的查询.
     * </p>
     *
     * <p>
     * size[0] 为总大小
     * </p>
     *
     * <p>
     * size[1] 为总文件数量
     * </p>
     */
    public static long[] getDirSize(File dir) {
        return new FileCounter().getFiles(dir);
    }

    public long getCompletedBytes() {
        return completedBytes.get();
    }

    public int getCompletedFiles() {
        return completedFiles.get();
    }
}
