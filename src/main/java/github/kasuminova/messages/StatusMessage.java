package github.kasuminova.messages;

public class StatusMessage extends AbstractMessage {
    private int used;
    private int total;
    private int max;
    private int runningThreadCount;
    private String clientIP;

    public StatusMessage(int used, int total, int max, int runningThreadCount, String clientIP) {
        this.used = used;
        this.total = total;
        this.max = max;
        this.runningThreadCount = runningThreadCount;
        this.clientIP = clientIP;
        messageType = StringMessage.class.getName();
    }

    public int getUsed() {
        return used;
    }

    public void setUsed(int used) {
        this.used = used;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getRunningThreadCount() {
        return runningThreadCount;
    }

    public void setRunningThreadCount(int runningThreadCount) {
        this.runningThreadCount = runningThreadCount;
    }

    public String getClientIP() {
        return clientIP;
    }

    public void setClientIP(String clientIP) {
        this.clientIP = clientIP;
    }
}
