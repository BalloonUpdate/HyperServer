package github.kasuminova.hyperserver.integratedserver;

import github.kasuminova.hyperserver.utils.NetworkLogger;

import javax.swing.*;

/**
 * LittleServer 面板向外开放的接口，大部分内容都在此处交互。
 */
public interface IntegratedServerInterface extends ServerInterface {
    NetworkLogger getLogger();

    JPanel getRequestListPanel();
}
