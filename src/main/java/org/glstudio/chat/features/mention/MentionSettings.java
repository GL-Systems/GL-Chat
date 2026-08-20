package org.glstudio.chat.features.mention;

import org.bukkit.Sound;
import org.glstudio.chat.core.util.Configs;
import org.glstudio.chat.core.util.SoundSetting;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public record MentionSettings(Type type, boolean highlightEnabled, String highlightFormat,
                              SoundSetting sound, boolean playToMentioned, boolean playToSender,
                              String bypassPermission) {
    public enum Type {
        ARROBA,

        NAME
    }

    public static final Pattern ARROBA_TOKEN = Pattern.compile("@([A-Za-z0-9_]{3,16})");
    public static final Pattern NAME_TOKEN = Pattern.compile("\\b([A-Za-z0-9_]{3,16})\\b");

    public static MentionSettings read(ConfigFile config) {
        return new MentionSettings(
                parseType(Configs.string(config, "mention_type", "ARROBA")),
                Configs.bool(config, "highlight.enabled", true),
                Configs.string(config, "highlight.format", "&e@%player%&r"),
                SoundSetting.read(config, "sound", true, Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
                Configs.bool(config, "options.play_sound_to_mentioned", true),
                Configs.bool(config, "options.play_sound_to_sender", false),
                Configs.string(config, "bypass_permission", "golden.chat.mention.bypass")
        );
    }

    private static Type parseType(String raw) {
        try {
            return Type.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LoggerUtils.logWarn("Unknown mention_type '" + raw + "'; falling back to ARROBA.");
            return Type.ARROBA;
        }
    }

    public Pattern tokenPattern() {
        return type == Type.ARROBA ? ARROBA_TOKEN : NAME_TOKEN;
    }
}
