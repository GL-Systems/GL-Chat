package org.glstudio.chat.core.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.nexus.utils.CC;

public final class StaffNotifier {
    private StaffNotifier() {
    }

    public static void notifySender(Player player, boolean enabled, String message) {
        if (!enabled || message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(CC.t(message));
    }

    public static void notifyStaff(JavaPlugin plugin, Player source, String originalMessage,
                                   boolean enabled, String permission, String format) {
        if (!enabled || format == null || format.isEmpty() || permission == null || permission.isEmpty()) {
            return;
        }

        String rendered = format
                .replace("%player%", source.getName())
                .replace("%message%", originalMessage)
                .replace("%original_message%", originalMessage);

        Broadcast.toPermission(plugin, permission, CC.component(rendered));
    }
}
