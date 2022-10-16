package github.kasuminova.hyperserver;

import cn.hutool.log.Log;
import cn.hutool.system.SystemUtil;
import github.kasuminova.hyperserver.configurations.ConfigurationManager;
import github.kasuminova.hyperserver.configurations.HyperServerConfig;
import github.kasuminova.hyperserver.remoteserver.RemoteServer;
import github.kasuminova.hyperserver.updatechecker.ApplicationVersion;
import github.kasuminova.messages.StatusMessage;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.*;

public class HyperServer {
    public static final ApplicationVersion VERSION = new ApplicationVersion("1.0.0-BETA");
    public static final Log logger = Log.get("main");
    public static final Timer GLOBAL_QUERY_TIMER = new Timer(false);
    public static final HyperServerConfig CONFIG = new HyperServerConfig();
    public static final Map<String, ChannelHandlerContext> connectedClientChannels = new HashMap<>();

    public static void main(String[] args) {
        logger.info("载入主配置文件...");
        loadConfig();

        logger.info("启动远控服务器...");
        RemoteServer remoteserver = new RemoteServer();
        remoteserver.start();

        GLOBAL_QUERY_TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                long memoryFree = SystemUtil.getFreeMemory();
                long memoryTotal = SystemUtil.getTotalMemory();
                long memoryMax = SystemUtil.getMaxMemory();

                connectedClientChannels.forEach((clientIP, ctx) -> ctx.writeAndFlush(new StatusMessage(
                        (int) ((memoryTotal - memoryFree) / (1024 * 1024)),
                        (int) (memoryTotal / (1024 * 1024)),
                        (int) (memoryMax / (1024 * 1024)),
                        Thread.activeCount(),
                        clientIP)));
            }
        }, 0, 1000);
    }

    private static void loadConfig() {
        String path = "hyperserver.json";
        try {
            if (new File(path).exists()) {
                ConfigurationManager.loadHyperServerConfigFromFile(path, CONFIG);
                logger.info("成功载入远控服务端配置文件.");
            } else {
                logger.info("远控服务端文件不存在, 正在创建文件...");

                char[] str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
                Random random = new Random();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 32; i++) {
                    int randomIndex = random.nextInt(str.length);
                    sb.append(str[randomIndex]);
                }

                logger.info("已生成随机 token: {}", sb);
                CONFIG.setToken(sb.toString());

                ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "hyperserver");
                logger.info("成功生成远控服务端配置文件.");
            }
        } catch (Exception e) {
            logger.error("远控服务端配置文件加载失败！", e);
        }
    }
}