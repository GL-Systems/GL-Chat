package org.glstudio.chat.core.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.EffectUtils;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.Locale;

public record SoundSetting(boolean enabled, Sound sound, float volume, float pitch) {
    public static final SoundSetting DISABLED = new SoundSetting(false, null, 0f, 0f);

    public static SoundSetting read(ConfigFile config, String path, boolean enabled, Sound fallback) {
        if (!enabled) {
            return DISABLED;
        }

        String raw = Configs.string(config, path + ".sound", null);
        Sound sound = resolve(raw, fallback, path);
        if (sound == null) {
            return DISABLED;
        }

        float volume = (float) Configs.number(config, path + ".volume", 1.0);
        float pitch = (float) Configs.number(config, path + ".pitch", 1.0);

        return new SoundSetting(true, sound, volume, pitch);
    }

    private static Sound resolve(String raw, Sound fallback, String path) {
        if (raw == null) {
            return fallback;
        }

        String key = raw.trim().toLowerCase(Locale.ROOT);
        NamespacedKey namespacedKey = NamespacedKey.fromString(key.indexOf(':') < 0
                ? NamespacedKey.MINECRAFT + ":" + key
                : key);

        Sound resolved = namespacedKey == null ? null : Registry.SOUNDS.get(namespacedKey);
        if (resolved != null) {
            return resolved;
        }

        String wanted = squash(key);
        for (Sound candidate : Registry.SOUNDS) {
            NamespacedKey candidateKey = Registry.SOUNDS.getKey(candidate);
            if (candidateKey == null) {
                continue;
            }
            if (squash(candidateKey.getKey()).equals(wanted)
                    || squash(candidateKey.toString()).equals(wanted)) {
                return candidate;
            }
        }

        LoggerUtils.logWarn("Unknown sound '" + raw + "' at " + path + ".sound; using the default.");
        return fallback;
    }

    static String squash(String key) {
        return key.toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
    }

    public void playTo(JavaPlugin plugin, Player player) {
        if (!enabled || sound == null) {
            return;
        }
        Broadcast.onMain(plugin, () -> {
            if (player.isOnline()) {
                EffectUtils.playSound(player, sound, volume, pitch);
            }
        });
    }

    public void playToAll(JavaPlugin plugin) {
        if (!enabled || sound == null) {
            return;
        }
        Broadcast.toAll(plugin, online -> EffectUtils.playSound(online, sound, volume, pitch));
    }
}
