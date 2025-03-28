package org.foresight;

public final class SecretsUtil {

    public static String readLichessURL() {
        return System.getenv("LICHESS_URL");
    }

    public static String readLichessSecret() {
        return System.getenv("LICHESS_SECRET");
    }

}
