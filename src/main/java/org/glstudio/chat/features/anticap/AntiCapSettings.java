package org.glstudio.chat.features.anticap;

import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.Locale;

public record AntiCapSettings(double maxCapsPercentage, int minMessageLength, String bypassPermission, Action action, String warnMessage, boolean notifyOnLowercase, String notifyLowercaseMessage) {
    public enum Action {
        WARN,
        LOWERCASE,
        BLOCK
    }

    public static AntiCapSettings read(ConfigFile config) {
        return new AntiCapSettings(
                Configs.number(config, "max_caps_percentage", 50),
                Math.max(1, Configs.integer(config, "min_message_length", 5)),
                Configs.string(config, "bypass_permission", "golden.chat.anticap.bypass"),
                parseAction(Configs.string(config, "action", "warn")),
                Configs.string(config, "warn_message", "&cPlease avoid using too many capital letters."),
                Configs.bool(config, "notify_on_lowercase", false),
                Configs.string(config, "notify_lowercase_message", "&7Your message was converted to lowercase.")
        );
    }

    private static Action parseAction(String raw) {
        try {
            return Action.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LoggerUtils.logWarn("Unknown anti-cap action '" + raw + "'; falling back to WARN.");
            return Action.WARN;
        }
    }

    public static double capsPercentage(String message) {
        int letters = 0;
        int caps = 0;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (!Character.isLetter(c)) {
                continue;
            }
            letters++;
            if (Character.isUpperCase(c)) {
                caps++;
            }
        }

        return letters == 0 ? 0 : (caps * 100.0) / letters;
    }
}