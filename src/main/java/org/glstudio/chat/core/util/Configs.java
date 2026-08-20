package org.glstudio.chat.core.util;

import org.glstudio.nexus.utils.ConfigFile;

import java.util.List;

public final class Configs {
    private Configs() {
    }

    public static String string(ConfigFile config, String path, String fallback) {
        if (!config.isExist(path)) {
            return fallback;
        }
        String value = config.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static int integer(ConfigFile config, String path, int fallback) {
        return config.isExist(path) ? config.getInt(path) : fallback;
    }

    public static double number(ConfigFile config, String path, double fallback) {
        return config.isExist(path) ? config.getDouble(path) : fallback;
    }

    public static boolean bool(ConfigFile config, String path, boolean fallback) {
        return config.isExist(path) ? config.getBoolean(path) : fallback;
    }

    public static List<String> list(ConfigFile config, String path) {
        return config.isExist(path) ? List.copyOf(config.getStringList(path)) : List.of();
    }
}
