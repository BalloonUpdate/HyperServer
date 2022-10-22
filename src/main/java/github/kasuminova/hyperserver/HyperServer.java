package github.kasuminova.hyperserver;

import cn.hutool.log.Log;
import cn.hutool.system.SystemUtil;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.configurations.HyperServerConfig;
import github.kasuminova.hyperserver.remoteserver.RemoteServer;
import github.kasuminova.balloonserver.updatechecker.ApplicationVersion;
import github.kasuminova.hyperserver.utils.RandomString;
import github.kasuminova.messages.StatusMessage;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.*;

public class HyperServer {
    public static final ApplicationVersion VERSION = new ApplicationVersion("1.0.0-BETA");
    public static final Log logger = Log.get("main");
    public static final Timer GLOBAL_QUERY_TIMER = new Timer(false);
    public static final HyperServerConfig CONFIG = new HyperServerConfig();
    public static final ApplicationVersion[] supportedClientVersions = new ApplicationVersion[]{
            new ApplicationVersion("1.4.0-BETA")
    };

    //已连接的客户端, key 值为 clientID
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

                connectedClientChannels.forEach((clientID, ctx) -> ctx.writeAndFlush(new StatusMessage(
                        (int) ((memoryTotal - memoryFree) / (1024 * 1024)),
                        (int) (memoryTotal / (1024 * 1024)),
                        (int) (memoryMax / (1024 * 1024)),
                        Thread.activeCount(),
                        clientID)));
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

                String token = RandomString.nextString(32);
                logger.info("已生成随机 token: {}", token);
                CONFIG.setToken(token);

                ConfigurationManager.saveConfigurationToFile(CONFIG, "./", "hyperserver");
                logger.info("成功生成远控服务端配置文件.");
            }
        } catch (Exception e) {
            logger.error("远控服务端配置文件加载失败！", e);
        }
    }
}