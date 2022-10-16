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
}
