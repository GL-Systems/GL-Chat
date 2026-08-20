package org.glstudio.chat.features.chatcooldown;

import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;

import java.util.List;

public record ChatCooldownSettings(String bypassPermission,
                                   boolean chatEnabled, int maxMessages, long windowMillis, String chatMessage,
                                   boolean commandsEnabled, long commandCooldownMillis, String commandMessage,
                                   List<String> watchedCommands) {
    public static ChatCooldownSettings read(ConfigFile config) {
        return new ChatCooldownSettings(
                Configs.string(config, "bypass_permission", "golden.chat.cooldown.bypass"),

                Configs.bool(config, "chat.enabled", true),

                Math.max(1, Configs.integer(config, "chat.max_messages", 1)),
                Math.max(0, Configs.integer(config, "chat.window_seconds", 3)) * 1000L,
                Configs.string(config, "chat.cooldown_message", "&cPlease wait before sending another message."),

                Configs.bool(config, "commands.enabled", true),
                Math.max(0, Configs.integer(config, "commands.cooldown_seconds", 1)) * 1000L,
                Configs.string(config, "commands.cooldown_message", "&cPlease wait before executing another command."),
                Configs.list(config, "commands.watched_commands")
        );
    }
}
