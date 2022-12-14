package github.kasuminova.balloonserver.utils.filecacheutils;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.JSONArray;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.balloonserver.utils.fileobject.AbstractSimpleFileObject;
import github.kasuminova.hyperserver.httpserver.HttpServerInterface;
import github.kasuminova.hyperserver.integratedserver.IntegratedServerInterface;
import github.kasuminova.hyperserver.utils.FileUtil;
import github.kasuminova.hyperserver.utils.HashCalculator;
import github.kasuminova.hyperserver.utils.NetworkLogger;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 从 LittleServer 的 cacheUtils 的内部类独立出来的一个工具，用于计算文件缓存并输出 JSON
 */
public class JsonCacheUtils {
    private final HttpServerInterface httpServerInterface;
    private final IntegratedServerInterface serverInterface;
    private final NetworkLogger logger;
    private final AtomicBoolean isGenerating;
    private final IntegratedServerConfig config;
    //jsonArray 转化为资源文件夹缓存必要的变量
    private final JSONArray jsonArray = new JSONArray();
    //fileObjList，用于序列化 JSON
    private final ArrayList<AbstractSimpleFileObject> fileObjList = new ArrayList<>();
    //线程池
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            0, Runtime.getRuntime().availableProcessors() * 2,
            0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    //公用定时器
    private Timer timer;
    private Thread counterThread;
    private FileCacheCalculator fileCacheCalculator;
    public JsonCacheUtils(IntegratedServerInterface serverInterface, HttpServerInterface httpServerInterface) {
        this.serverInterface = serverInterface;
        this.httpServerInterface = httpServerInterface;

        logger = serverInterface.getLogger();
        isGenerating = serverInterface.isGenerating();
        config = serverInterface.getIntegratedServerConfig();
    }

    /**
     * 更新资源缓存结构
     * 传入的参数如果为 null，则完整生成一次缓存
     */
    public void updateDirCache(JSONArray jsonCache, String hashAlgorithm) {
        long start = System.currentTimeMillis();

        logger.info(String.format("正在计算 %s 资源缓存结构中...", hashAlgorithm));
        if (jsonCache != null) {
            if (!genDirCache(jsonCache, hashAlgorithm)) {
                return;
            }
            //等待线程结束
            ThreadUtil.waitForDie(counterThread);

            executor.shutdown();
            timer.stop();
            logger.info(String.format("资源变化计算完毕, 正在向磁盘生成 JSON 缓存. (%sms)", System.currentTimeMillis() - start));

            //输出并向服务器设置 JSON
            generateJsonToDiskAndSetServerJson(jsonArray, hashAlgorithm);

            //重置变量
            isGenerating.set(false);
        } else if (genDirCache(null, hashAlgorithm)) {
            //等待线程结束
            ThreadUtil.waitForDie(counterThread);

            executor.shutdown();
            timer.stop();
            logger.info(String.format("资源变化计算完毕, 正在向磁盘生成 JSON 缓存. (%sms)", System.currentTimeMillis() - start));

            //输出并向服务器设置 JSON
            jsonArray.clear();
            jsonArray.addAll(fileObjList);
            generateJsonToDiskAndSetServerJson(jsonArray, hashAlgorithm);

            //重置变量
            isGenerating.set(false);
        }
    }

    /**
     * <p>
     * 更新资源缓存结构并启动服务器。
     * </p>
     * <p>
     * 传入的参数如果为 null，则完整生成一次缓存。
     * </p>
     */
    public void updateDirCacheAndStartServer(JSONArray jsonCache, String hashAlgorithm) {
        updateDirCache(jsonCache, hashAlgorithm);

        System.gc();
        logger.info("内存已完成回收.");

        //启动服务器
        if (httpServerInterface.startServer()) {
            serverInterface.isStarted().set(true);
        }
    }

    /**
     * 向磁盘生成 JSON 缓存，并设置服务端 JSON
     *
     * @param jsonArray JSON 缓存
     */
    private void generateJsonToDiskAndSetServerJson(JSONArray jsonArray, String hashAlgorithm) {
        String resJSONStr = jsonArray.toJSONString();
        try {
            if (hashAlgorithm.equals(HashCalculator.SHA1)) {
                serverInterface.setLegacyResJson(resJSONStr);
                FileUtil.createJsonFile(resJSONStr, "./", String.format("%s.%s",
                        serverInterface.getServerName(),
                        serverInterface.getLegacyResJsonFileExtensionName()));
            } else {
                serverInterface.setResJson(resJSONStr);
                FileUtil.createJsonFile(resJSONStr, "./", String.format("%s.%s",
                        serverInterface.getServerName(),
                        serverInterface.getResJsonFileExtensionName()));
            }
            logger.info("JSON 缓存生成完毕.");
        } catch (IORuntimeException ex) {
            logger.error("生成 JSON 缓存的时候出现了问题...", ex);
        }
    }

    /**
     * 以多线程方式生成资源缓存结构
     * <p>
     * 使用配置文件中的资源文件夹路径
     * </p>
     *
     * @return 如果资源文件夹为空返回 false, 否则返回 true
     */
    private boolean genDirCache(JSONArray jsonCache, String hashAlgorithm) {
        File dir = new File("." + config.getMainDirPath());
        if (!dir.exists()) {
            logger.warn(String.format("设定中的资源目录: %s 不存在, 使用默认路径", dir.getPath()));
            dir = new File("./res");
            if (!dir.exists()) {
                logger.warn(String.format("默认资源目录不存在: %s, 正在创建文件夹", dir.getPath()));
                if (!dir.mkdir()) {
                    logger.error("默认资源目录创建失败！请检查你的资源目录文件夹是否被占用或非文件夹。");
                }
                logger.warn("资源目录为空, 跳过缓存生成.");

                return false;
            }
        }

        logger.info("正在计算资源目录大小...");
        File[] fileList = dir.listFiles();
        //检查文件夹是否为空
        if (fileList == null || fileList.length == 0) {
            logger.warn("资源目录为空, 跳过缓存生成.");

            return false;
        }

        isGenerating.set(true);
        //计算文件夹内的文件和总大小（文件夹不计入），用于进度条显示
        long[] dirSize = FileCacheCalculator.getDirSize(dir);

        String totalSize = FileUtil.formatFileSizeToStr(dirSize[0]);

        logger.info(String.format("文件夹大小: %s, 文件数量: %s", totalSize, dirSize[1]));

        if (jsonCache != null) {
            logger.info("检测到已缓存的 JSON, 正在检查变化...");
            checkJsonCache(dir, jsonCache, dirSize[1], hashAlgorithm);
        } else {
            logger.info("正在生成资源目录缓存...");
            generateCache(dir, hashAlgorithm, totalSize, dirSize[0], dirSize[1]);
        }

        return true;
    }

    private void checkJsonCache(File dir, JSONArray jsonCache, long totalFiles, String hashAlgorithm) {
        //创建新线程实例并执行
        AtomicInteger completedFiles = new AtomicInteger(0);
        jsonArray.clear();

        counterThread = new Thread(() -> jsonArray.addAll(new JsonCacheCheckerTask(
                dir, AbstractSimpleFileObject.jsonArrToFileObjArr(jsonCache), hashAlgorithm, logger, completedFiles, executor)
                .call().getChildren()));
        counterThread.start();

        timer = new Timer(250, e -> {
//            statusProgressBar.setValue((int) ((double) completedFiles.get() * 1000 / totalFiles));
//            statusProgressBar.setString(String.format("检查变化中: %s 文件 / %s 文件", completedFiles.get(), totalFiles));
        });
        //启动轮询
        timer.start();
    }

    private void generateCache(File dir, String hashAlgorithm, String totalSize, long totalFileSize, long totalFiles) {
        //新建资源计算器实例
        fileCacheCalculator = new FileCacheCalculator(hashAlgorithm);
        //创建新线程实例并执行
        counterThread = new Thread(() -> {
            fileObjList.clear();
            fileObjList.addAll(fileCacheCalculator.scanDir(dir, logger));
        });
        counterThread.start();

        //轮询线程, 读取进度
        timer = new Timer(250, e -> {
            long completedBytes = fileCacheCalculator.getCompletedBytes();
            long completedFiles = fileCacheCalculator.getCompletedFiles();
            String completedSize = FileUtil.formatFileSizeToStr(completedBytes);
//            statusProgressBar.setValue((int) ((double) completedBytes * 1000 / totalFileSize));
//            statusProgressBar.setString(String.format("生成缓存中: %s / %s - %s 文件 / %s 文件",
//                    completedSize,
//                    totalSize,
//                    completedFiles,
//                    totalFiles));
        });
        //启动轮询
        timer.start();
    }
}
