package github.kasuminova.hyperserver.integratedserver;

import com.alibaba.fastjson2.JSONObject;
import github.kasuminova.balloonserver.configurations.IntegratedServerConfig;
import github.kasuminova.hyperserver.httpserver.HttpServer;

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
}
