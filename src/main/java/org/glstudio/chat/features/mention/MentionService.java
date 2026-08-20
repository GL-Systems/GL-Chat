package org.glstudio.chat.features.mention;

import org.glstudio.nexus.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.chat.ChatProcessor;
import org.glstudio.chat.core.chat.ChatResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class MentionService implements ChatProcessor {
    private final Chat plugin;

    public MentionService(Chat plugin) {
        this.plugin = plugin;
    }

    private MentionSettings settings() {
        return plugin.getConfigManager().getMention();
    }

    public record Mention(int start, int end, String playerName) {
    }

    @Override
    public ChatResult process(Player sender, String message) {
        MentionSettings settings = settings();

        for (Mention mention : find(message, settings)) {
            Player target = Bukkit.getPlayerExact(mention.playerName());
            if (target == null || target.hasPermission(settings.bypassPermission())) {
                continue;
            }

            if (settings.playToMentioned()) {
                settings.sound().playTo(plugin, target);
            }
            if (settings.playToSender()) {
                settings.sound().playTo(plugin, sender);
            }
        }

        return ChatResult.allow(message);
    }

    public Component highlight(String message, boolean allowColor) {
        MentionSettings settings = settings();

        if (!settings.highlightEnabled()) {
            return segment(message, allowColor);
        }

        List<Mention> mentions = find(message, settings);
        if (mentions.isEmpty()) {
            return segment(message, allowColor);
        }

        Component result = Component.empty();
        int cursor = 0;

        for (Mention mention : mentions) {
            if (mention.start() > cursor) {
                result = result.append(segment(message.substring(cursor, mention.start()), allowColor));
            }

            result = result.append(
                    CC.component(settings.highlightFormat().replace("%player%", mention.playerName())));
            cursor = mention.end();
        }

        if (cursor < message.length()) {
            result = result.append(segment(message.substring(cursor), allowColor));
        }

        return result;
    }

    private Component segment(String text, boolean allowColor) {
        return CC.componentOfUserInput(text, allowColor);
    }

    private List<Mention> find(String message, MentionSettings settings) {
        List<Mention> mentions = new ArrayList<>();
        Matcher matcher = settings.tokenPattern().matcher(message);

        while (matcher.find()) {
            String name = matcher.group(1);
            Player target = Bukkit.getPlayerExact(name);
            if (target == null) {
                continue;
            }

            mentions.add(new Mention(matcher.start(), matcher.end(), target.getName()));
        }

        return mentions;
    }
}
