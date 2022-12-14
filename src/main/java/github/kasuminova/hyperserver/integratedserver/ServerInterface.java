package github.kasuminova.hyperserver.integratedserver;

import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;

import java.util.concurrent.atomic.AtomicBoolean;

public interface ServerInterface {
    IntegratedServerConfig getIntegratedServerConfig();

    String getServerName();

    /**
     * 重新生成缓存
     */
    void regenCache();

    /**
     * 关闭服务器
     *
     * @return 是否关闭成功
     */
    boolean stopServer();

    /**
     * 保存配置
     */
    void saveConfig();

    //获取 index.json 字符串
    String getIndexJson();

    String getResJsonFileExtensionName();

    String getLegacyResJsonFileExtensionName();

    //设置新的旧版文件结构 JSON
    void setLegacyResJson(String newLegacyResJson);

    //获取旧版文件结构 JSON
    String getLegacyResJson();

    //设置新的文件结构 JSON
    void setResJson(String newResJson);

    AtomicBoolean isGenerating();

    AtomicBoolean isStarted();

    //获取文件结构 JSON
    String getResJson();
}