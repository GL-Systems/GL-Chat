package org.glstudio.chat.features.commandspy;

import org.glstudio.nexus.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.util.Broadcast;
import org.glstudio.chat.core.util.CommandNames;
import org.glstudio.chat.core.util.Placeholders;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CommandSpyService {
    private final Chat plugin;

    private final Set<UUID> muted = ConcurrentHashMap.newKeySet();

    public CommandSpyService(Chat plugin) {
        this.plugin = plugin;
    }

    private CommandSpySettings settings() {
        return plugin.getConfigManager().getCommandSpy();
    }

    public void handleCommand(Player player, String command) {
        CommandSpySettings settings = settings();

        if (CommandNames.matches(command, settings.blacklisted())) return;
        if (!settings.whitelisted().isEmpty() && !CommandNames.matches(command, settings.whitelisted())) return;

        Component message = CC.component(Placeholders.apply(player, settings.format(),
                Map.of("%command%", command, "%time%", Placeholders.timeWithSeconds())));

        Broadcast.toAll(plugin, online -> {
            if (online.getUniqueId().equals(player.getUniqueId())) return;
            if (!online.hasPermission(settings.spyPermission())) return;
            if (muted.contains(online.getUniqueId())) return;
            online.sendMessage(message);
        });

        if (settings.spyToConsole()) {
            Broadcast.onMain(plugin, () -> Bukkit.getConsoleSender().sendMessage(message));
        }
    }

    public boolean toggleSpy(Player player) {
        if (muted.remove(player.getUniqueId())) {
            return true;
        }
        muted.add(player.getUniqueId());
        return false;
    }

    public boolean isSpyEnabled(Player player) {
        return !muted.contains(player.getUniqueId());
    }

    public void handleQuit(Player player) {
        muted.remove(player.getUniqueId());
    }

    public void clear() {
        muted.clear();
    }
}
