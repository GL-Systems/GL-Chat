package org.glstudio.chat.features.chatfilter;

import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.chat.ChatProcessor;
import org.glstudio.chat.core.chat.ChatResult;
import org.glstudio.chat.core.util.StaffNotifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilterService implements ChatProcessor {
    private final Chat plugin;

    public ChatFilterService(Chat plugin) {
        this.plugin = plugin;
    }

    private ChatFilterSettings settings() {
        return plugin.getConfigManager().getChatFilter();
    }

    @Override
    public ChatResult process(Player player, String message) {
        ChatFilterSettings settings = settings();

        if (settings.patterns().isEmpty()) return ChatResult.allow(message);
        if (player.hasPermission(settings.bypassPermission())) return ChatResult.allow(message);

        int matches = 0;
        for (Pattern pattern : settings.patterns()) {
            if (pattern.matcher(message).find()) {
                matches++;
            }
        }

        if (matches == 0) return ChatResult.allow(message);

        notify(player, message, settings);

        if (matches >= settings.blockThreshold() || settings.mode() == ChatFilterSettings.Mode.BLOCK) {
            return ChatResult.cancel();
        }

        String replacement = settings.mode() == ChatFilterSettings.Mode.REMOVE ? "" : settings.replacement();
        String filtered = message;
        for (Pattern pattern : settings.patterns()) {
            filtered = pattern.matcher(filtered).replaceAll(Matcher.quoteReplacement(replacement));
        }

        filtered = filtered.strip();

        return filtered.isEmpty() ? ChatResult.cancel() : ChatResult.allow(filtered);
    }

    private void notify(Player player, String original, ChatFilterSettings settings) {
        StaffNotifier.notifySender(player, settings.notifyPlayer(), settings.notifyMessage());
        StaffNotifier.notifyStaff(plugin, player, original, settings.notifyStaff(),
                settings.staffPermission(), settings.staffMessage());
    }
}
