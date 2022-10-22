package github.kasuminova.balloonserver.configurations;

import com.alibaba.fastjson2.annotation.JSONField;

public class HyperServerConfig extends Configuration {
    //自动启动服务器
    @JSONField(ordinal = 1)
    private boolean autoStartServer = false;
    //自动启动服务器（仅一次）
    @JSONField(ordinal = 2)
    private boolean autoStartServerOnce = false;
    //DEBUG 模式
    @JSONField(ordinal = 3)
    private boolean debugMode = false;
    //token
    @JSONField(ordinal = 4)
    private String token = "";

    public HyperServerConfig() {
        configVersion = 0;
    }

    @Override
    public HyperServerConfig setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
        return this;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public HyperServerConfig setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        return this;
    }

    public boolean isAutoStartServer() {
        return autoStartServer;
    }

    public HyperServerConfig setAutoStartServer(boolean autoStartServer) {
        this.autoStartServer = autoStartServer;
        return this;
    }

    public boolean isAutoStartServerOnce() {
        return autoStartServerOnce;
    }

    public HyperServerConfig setAutoStartServerOnce(boolean autoStartServerOnce) {
        this.autoStartServerOnce = autoStartServerOnce;
        return this;
    }

    public String getToken() {
        return token;
    }

    public HyperServerConfig setToken(String token) {
        this.token = token;
        return this;
    }
}
