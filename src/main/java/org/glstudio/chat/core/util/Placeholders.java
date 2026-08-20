package org.glstudio.chat.core.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class Placeholders {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private Placeholders() {
    }

    public static String apply(Player player, String text) {
        return apply(player, text, Map.of());
    }

    public static String apply(Player player, String text, Map<String, String> extra) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        LocalTime now = LocalTime.now();
        result = result
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%player_displayname%", PLAIN.serialize(player.displayName()))
                .replace("%world%", player.getWorld().getName())
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%time_seconds%", now.format(TIME_SECONDS))
                .replace("%time%", now.format(TIME));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            result = PlaceholderAPI.setPlaceholders(player, result);
        }

        return result;
    }

    public static String time() {
        return LocalTime.now().format(TIME);
    }

    public static String timeWithSeconds() {
        return LocalTime.now().format(TIME_SECONDS);
    }
}
