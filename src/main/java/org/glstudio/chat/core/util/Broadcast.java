package org.glstudio.chat.core.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.nexus.utils.Tasks;

import java.util.function.Consumer;

public final class Broadcast {

    private Broadcast() {
    }

    public static void toAll(JavaPlugin plugin, Consumer<Player> action) {
        onMain(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                action.accept(online);
            }
        });
    }

    public static void message(JavaPlugin plugin, Component component) {
        onMain(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(component);
            }
            Bukkit.getConsoleSender().sendMessage(component);
        });
    }

    public static void toPermission(JavaPlugin plugin, String permission, Component message) {
        if (permission == null || permission.isEmpty()) {
            return;
        }
        toAll(plugin, online -> {
            if (online.hasPermission(permission)) {
                online.sendMessage(message);
            }
        });
    }

    public static void onMain(JavaPlugin plugin, Runnable action) {
        new Tasks(plugin).runOnMain(action);
    }
}
