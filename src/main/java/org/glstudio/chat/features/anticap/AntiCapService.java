package org.glstudio.chat.features.anticap;

import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.chat.ChatProcessor;
import org.glstudio.chat.core.chat.ChatResult;
import org.glstudio.nexus.utils.CC;

public class AntiCapService implements ChatProcessor {
    private final Chat plugin;

    public AntiCapService(Chat plugin) {
        this.plugin = plugin;
    }

    private AntiCapSettings settings() {
        return plugin.getConfigManager().getAntiCap();
    }

    @Override
    public ChatResult process(Player player, String message) {
        AntiCapSettings settings = settings();

        if (player.hasPermission(settings.bypassPermission())) return ChatResult.allow(message);
        if (message.length() < settings.minMessageLength()) return ChatResult.allow(message);

        if (AntiCapSettings.capsPercentage(message) <= settings.maxCapsPercentage()) {
            return ChatResult.allow(message);
        }

        return switch (settings.action()) {
            case BLOCK -> {
                player.sendMessage(CC.t(settings.warnMessage()));
                yield ChatResult.cancel();
            }
            case LOWERCASE -> {
                if (settings.notifyOnLowercase()) {
                    player.sendMessage(CC.t(settings.notifyLowercaseMessage()));
                }
                yield ChatResult.allow(message.toLowerCase());
            }
            case WARN -> {
                player.sendMessage(CC.t(settings.warnMessage()));
                yield ChatResult.allow(message);
            }
        };
    }
}