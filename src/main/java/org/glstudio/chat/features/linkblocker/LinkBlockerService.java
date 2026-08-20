package org.glstudio.chat.features.linkblocker;

import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.chat.ChatProcessor;
import org.glstudio.chat.core.chat.ChatResult;
import org.glstudio.chat.core.util.StaffNotifier;
import org.glstudio.nexus.utils.CC;

import java.util.regex.Matcher;

public class LinkBlockerService implements ChatProcessor {
    private final Chat plugin;

    public LinkBlockerService(Chat plugin) {
        this.plugin = plugin;
    }

    private LinkBlockerSettings settings() {
        return plugin.getConfigManager().getLinkBlocker();
    }

    @Override
    public ChatResult process(Player player, String message) {
        LinkBlockerSettings settings = settings();

        if (player.hasPermission(settings.bypassPermission())) return ChatResult.allow(message);

        Matcher matcher = LinkBlockerSettings.URL_PATTERN.matcher(message);

        boolean blocked = false;
        StringBuilder result = new StringBuilder();
        String replacement = CC.t(settings.replacement());

        while (matcher.find()) {
            String url = matcher.group();

            if (LinkBlockerSettings.isWhitelisted(url, settings.whitelistedDomains())) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(url));
                continue;
            }

            blocked = true;

            if (settings.action() == LinkBlockerSettings.Action.BLOCK) {
                notify(player, message, settings);
                return ChatResult.cancel();
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        if (!blocked) return ChatResult.allow(message);

        matcher.appendTail(result);
        notify(player, message, settings);
        return ChatResult.allow(result.toString());
    }

    private void notify(Player player, String original, LinkBlockerSettings settings) {
        StaffNotifier.notifySender(player, settings.notifyPlayer(), settings.notifyMessage());
        StaffNotifier.notifyStaff(plugin, player, original, settings.notifyStaff(),
                settings.staffPermission(), settings.staffMessage());
    }
}
