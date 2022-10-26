package github.kasuminova.hyperserver.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MiscUtils {
    /**
     * <p>
     * 将错误打印成字符串。
     * </p>
     * <p>
     * 类似 printStackTrace()。
     * </p>
     *
     * @param e Exception
     * @return 字符串
     */
    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    public static String formatTime(long time) {
        if (time < 1000 * 10) {
            return String.format("%.3fs", (double) time / 1000);
        } else if (time < 1000 * 100) {
            return String.format("%.2fs", (double) time / 1000);
        } else if (time < 1000 * 1000) {
            return String.format("%.1fs", (double) time / 1000);
        } else {
            return String.format("%ss", time / 1000);
        }
    }
}
