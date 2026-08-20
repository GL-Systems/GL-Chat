package org.glstudio.chat.features.chatcontrol;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.Permissions;
import org.glstudio.chat.core.chat.ChatProcessor;
import org.glstudio.chat.core.chat.ChatResult;
import org.glstudio.chat.core.util.Broadcast;
import org.glstudio.chat.core.util.CommandNames;
import org.glstudio.nexus.utils.CC;

@Setter
@Getter
public class ChatControlService implements ChatProcessor {
    private static final int CLEAR_LINES = 100;

    private final Chat plugin;

    private volatile boolean chatEnabled = true;

    public ChatControlService(Chat plugin) {
        this.plugin = plugin;
    }

    public boolean toggle() {
        chatEnabled = !chatEnabled;
        return chatEnabled;
    }

    @Override
    public ChatResult process(Player player, String message) {
        if (chatEnabled) return ChatResult.allow(message);
        if (player.hasPermission(Permissions.BYPASS)) return ChatResult.allow(message);

        player.sendMessage(CC.t(plugin.getConfigManager().getMessage("chat_cmd.chat_disabled")));
        return ChatResult.cancel();
    }

    public boolean isCommandBlocked(Player player, String commandLine) {
        if (chatEnabled) return false;
        if (player.hasPermission(Permissions.BYPASS)) return false;

        if (!CommandNames.matches(commandLine, plugin.getConfigManager().getBlockedCommandsWhenChatOff())) {
            return false;
        }

        player.sendMessage(CC.t(plugin.getConfigManager().getMessage("chat_cmd.chat_disabled")));
        return true;
    }

    public void clear(CommandSender executor) {
        Broadcast.toAll(plugin, online -> {
            if (online.hasPermission(Permissions.ADMIN)) {
                return;
            }
            for (int i = 0; i < CLEAR_LINES; i++) {
                online.sendMessage(Component.empty());
            }
        });

        String cleared = plugin.getConfigManager().getMessage("chat_cmd.cleared")
                .replace("%player%", executor.getName());
        Broadcast.message(plugin, CC.component(cleared));
    }
}