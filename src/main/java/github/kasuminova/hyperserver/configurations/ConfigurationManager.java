package github.kasuminova.hyperserver.configurations;

import cn.hutool.core.io.IORuntimeException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import github.kasuminova.hyperserver.utils.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Kasumi_Nova
 */
public class ConfigurationManager {
    public static void loadIntegratedServerConfigFromFile(String path, IntegratedServerConfig oldConfig) throws IOException {
        IntegratedServerConfig newConfig = JSON.parseObject(Files.newInputStream(Paths.get(path)), IntegratedServerConfig.class);

        oldConfig.setConfigVersion(newConfig.getConfigVersion())
                .setIp(newConfig.getIp())
                .setPort(newConfig.getPort())
                .setMainDirPath(newConfig.getMainDirPath())
                .setFileChangeListener(newConfig.isFileChangeListener())
                .setCompatibleMode(newConfig.isCompatibleMode())
                .setJksFilePath(newConfig.getJksFilePath())
                .setJksSslPassword(newConfig.getJksSslPassword())
                .setCommonMode(newConfig.getCommonMode())
                .setOnceMode(newConfig.getOnceMode());
    }

    public static void loadHyperServerConfigFromFile(String path, HyperServerConfig oldConfig) throws IOException {
        HyperServerConfig config = JSON.parseObject(Files.newInputStream(Paths.get(path)), HyperServerConfig.class);
        oldConfig.setAutoStartServer(config.isAutoStartServer())
                .setAutoStartServerOnce(config.isAutoStartServerOnce())
                .setDebugMode(config.isDebugMode())
                .setToken(config.getToken());
    }

    public static void saveConfigurationToFile(Configuration configuration, String path, String name) throws IORuntimeException {
        FileUtil.createJsonFile(JSONObject.toJSONString(configuration, JSONWriter.Feature.PrettyFormat), path, name);
    }
}
