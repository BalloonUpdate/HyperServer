package github.kasuminova.hyperserver.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import github.kasuminova.hyperserver.HyperServer;
import github.kasuminova.messages.LogMessage;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkLogger {
    //日期格式
    private static final SimpleDateFormat NORMAL_DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat FULL_DATE_FORMAT   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public static final String INFO  = "INFO";
    public static final String WARN  = "WARN";
    public static final String ERROR = "ERROR";
    public static final String DEBUG = "DEBUG";
    private final Log logger;
    private final Writer logWriter;

    /**
     * logger 线程池
     * 用于保证 Log 顺序
     */
    private final ExecutorService loggerThreadPool = Executors.newSingleThreadExecutor();
    private final AtomicInteger prepared = new AtomicInteger(0);

    /**
     * 创建一个 Logger
     *
     * @param name log 文件名
     */
    public NetworkLogger(String name) {
        logger = LogFactory.get(name);

        logWriter = createLogFile(name, logger);
    }

    public void log(String msg, String level, Object... params) {
        if (prepared.get() > 100) return;

        String threadName = Thread.currentThread().getName();

        prepared.getAndIncrement();
        loggerThreadPool.execute(() -> {
            String formatMsg = StrUtil.format(msg, params);

            writeAndFlushMessage(level, threadName, formatMsg);
            prepared.getAndDecrement();
        });
    }

    /**
     * 输出 INFO 日志
     *
     * @param msg 消息
     */
    public void info(String msg) {
        logger.info(msg);
        log(msg, INFO);
    }

    /**
     * 输出 INFO 日志
     *
     * @param format    消息
     * @param params    占位符
     */
    public void info(String format, Object... params) {
        logger.info(format, params);
        log(format, INFO, params);
    }

    /**
     * 输出 DEBUG 日志
     *
     * @param msg 消息
     */
    public void debug(String msg) {
        logger.debug(msg);
        log(msg, DEBUG);
    }

    public void debug(String format, Object... params) {
        logger.debug(format, params);
        log(format, DEBUG, params);
    }

    /**
     * 输出 WARN 日志
     *
     * @param msg 消息
     */
    public void warn(String msg) {
        logger.warn(msg);
        log(msg, WARN);
    }

    public void warn(String format, Object... params) {
        logger.warn(format, params);
        log(format, WARN, params);
    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     */
    public void error(String msg) {
        logger.error(msg);
        log(msg, ERROR);
    }

    public void error(String format, Object... params) {
        logger.error(format, params);
        log(format, ERROR, params);
    }

    /**
     * 输出 ERROR 日志
     *
     * @param msg 消息
     * @param e   错误信息
     */
    public void error(String msg, Throwable e) {
        String stackTrace = MiscUtils.stackTraceToString(e);
        logger.error(msg, stackTrace);
        log(msg, ERROR, stackTrace);
    }

    /**
     * 输出 ERROR 日志
     *
     * @param e 错误信息
     */
    public void error(Throwable e) {
        String stackTrace = MiscUtils.stackTraceToString(e);
        logger.error(stackTrace);
        log("", ERROR, stackTrace);
    }

    /**
     * 关闭 Writer, 解除 log 文件的占用
     */
    public void closeLogWriter() throws IOException {
        loggerThreadPool.shutdown();
        if (logWriter == null) return;
        logWriter.close();
    }

    /**
     * 向 log 文件和已连接的客户端输出日志
     *
     * @param logMessage 要输出的内容
     */
    private void writeAndFlushMessage(String level, String threadName, String logMessage) {
        HyperServer.connectedClientChannels.forEach((key, value) -> value.writeAndFlush(new LogMessage(level, logMessage)));
        if (logWriter == null) return;
        try {
            logWriter.write(buildFullLogMessage(threadName, logMessage, level));
            logWriter.flush();
        } catch (IOException ignored) {}
    }

    private static String buildNormalLogMessage(String msg, String logLevel) {
        //占位符分别为 时间，消息等级，消息本体
        return String.format("[%s][%s]: %s\n", NORMAL_DATE_FORMAT.format(System.currentTimeMillis()), logLevel, msg);
    }

    private static String buildFullLogMessage(String threadName, String msg, String logLevel) {
        //占位符分别为 日期+时间，消息等级，线程名，消息本体
        return String.format("[%s][%s][%s]: %s\n", FULL_DATE_FORMAT.format(System.currentTimeMillis()), logLevel, threadName, msg);
    }

    /**
     * <p>
     * 创建一个日志文件。
     * </p>
     * <p>
     * 如果日志文件已存在，则将其有序的重命名（最多保留 50 个）。
     * </p>
     *
     * @param name   文件名
     * @param logger 如果出现问题, 则使用此 logger 警告
     * @return 一个对应文件名的 Writer, 如果出现问题, 则返回 null
     */
    public static Writer createLogFile(String name, Log logger) {
        File logFile = new File(String.format("./logs/%s.log", name));
        try {
            if (logFile.exists()) {
                int count = 1;
                while (count <= 50) {
                    File oldLogFile = new File(String.format("./logs/%s-%s.log", name, count));
                    if (oldLogFile.exists()) {
                        count++;
                    } else {
                        if (!logFile.renameTo(oldLogFile)) {
                            logger.warn("无法正确转移旧日志文件！");
                        }
                        break;
                    }
                }
                return new OutputStreamWriter(Files.newOutputStream(new File(String.format("./logs/%s.log", name)).toPath()), StandardCharsets.UTF_8);
            }

            //检查父文件夹
            if (!logFile.getParentFile().exists()) {
                try {
                    FileUtil.touch(logFile);
                } catch (IORuntimeException e) {
                    logger.warn("日志文件夹创建失败！{}", MiscUtils.stackTraceToString(e));
                    return null;
                }
            }

            return new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn(MiscUtils.stackTraceToString(e));
        }
        return null;
    }
}
