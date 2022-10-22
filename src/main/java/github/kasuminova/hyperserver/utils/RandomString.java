package github.kasuminova.hyperserver.utils;

import java.util.Random;

public class RandomString {
    private static final Random random = new Random();
    private static final char[] str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    public static String nextString(int length) {
        if (length < 1 || length > 10000) throw new IllegalArgumentException("length too small or too big!");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(str.length);
            sb.append(str[randomIndex]);
        }
        return sb.toString();
    }
}
