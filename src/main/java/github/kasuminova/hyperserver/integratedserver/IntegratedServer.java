package github.kasuminova.hyperserver.integratedserver;

import cn.hutool.core.io.IORuntimeException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import github.kasuminova.balloonserver.configurations.ConfigurationManager;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.hyperserver.httpserver.HttpServer;
import github.kasuminova.hyperserver.utils.HashCalculator;

import java.io.File;
import java.io.IOException;

import static github.kasuminova.hyperserver.HyperServer.logger;

public class IntegratedServer {
    protected final JSONObject index = new JSONObject();
    protected final String resJsonFileExtensionName = "res-cache";
    protected final String legacyResJsonFileExtensionName = "legacy_res-cache";
    protected final String configFileSuffix = ".lscfg.json";
    protected final String serverName;
    protected final IntegratedServerConfig config = new IntegratedServerConfig();
    protected HttpServer server;
    protected String indexJson = null;
    protected String resJson = null;
    protected String legacyResJson = null;

    public IntegratedServer(String serverName) {
        this.serverName = serverName;


    }

    /**
     * 保存配置文件至磁盘
     */
    protected void saveConfigurationToFile() {
        try {
            ConfigurationManager.saveConfigurationToFile(config, "./", serverName + configFileSuffix.replace(".json", ""));
            logger.info("已保存配置至磁盘.");
        } catch (IORuntimeException ex) {
            logger.error("保存配置文件的时候出现了问题...", ex);
        }
    }

    /**
     * 从文件加载配置文件
     */
    protected void loadConfigurationFromFile() {
        if (!new File("./" + serverName + configFileSuffix).exists()) {
            try {
                logger.warn("未找到配置文件，正在尝试在程序当前目录生成配置文件...");
                ConfigurationManager.saveConfigurationToFile(new IntegratedServerConfig(), "./", String.format("%s.lscfg", serverName));
                logger.info("配置生成成功.");
                logger.info("目前正在使用程序默认配置.");
            } catch (Exception e) {
                logger.error("生成配置文件的时候出现了问题...", e);
                logger.info("目前正在使用程序默认配置.");
            }
            return;
        }
        try {
            ConfigurationManager.loadIntegratedServerConfigFromFile("./" + serverName + configFileSuffix, config);
        } catch (IOException e) {
            logger.error("加载配置文件的时候出现了问题...", e);
            logger.info("目前正在使用程序默认配置.");
            return;
        }

        reloadIndexJson();
        logger.info("已载入配置文件.");
    }

    /**
     * 重新构建 index.json 字符串缓存
     */
    protected void reloadIndexJson() {
        index.clear();
        index.put("update", config.getMainDirPath().replace("/", "").intern());
        index.put("hash_algorithm", HashCalculator.CRC32);
        index.put("common_mode", config.getCommonMode());
        index.put("once_mode", config.getOnceMode());

        indexJson = index.toJSONString(JSONWriter.Feature.PrettyFormat);
    }
}
