package org.glstudio.chat.core.chat;

import org.glstudio.nexus.utils.CC;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.Permissions;
import org.glstudio.chat.features.chatformat.ChatFormatService;
import org.glstudio.chat.features.globalchat.GlobalChatService;
import org.glstudio.chat.features.mention.MentionService;

public class ChatListener implements Listener {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final Chat plugin;

    public ChatListener(Chat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        ChatResult result = plugin.getChatPipeline().process(player, PLAIN.serialize(event.message()));

        if (result.cancelled()) {
            event.setCancelled(true);
            return;
        }

        event.message(buildMessage(player, result.message()));

        ChatFormatService format = plugin.getChatFormatService();
        if (format != null) {
            event.renderer(renderer(format));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDelivered(AsyncChatEvent event) {
        Player player = event.getPlayer();

        plugin.getChatPipeline().delivered(player);

        GlobalChatService globalChat = plugin.getGlobalChatService();
        if (globalChat == null) {
            return;
        }

        Component rendered = event.renderer()
                .render(player, player.displayName(), event.message(), Bukkit.getConsoleSender());
        globalChat.publish(player, rendered);
    }

    private Component buildMessage(Player player, String message) {
        boolean allowColor = player.hasPermission(Permissions.COLOR);

        MentionService mention = plugin.getMentionService();
        if (mention != null) {
            return mention.highlight(message, allowColor);
        }

        return CC.componentOfUserInput(message, allowColor);
    }

    private ChatRenderer renderer(ChatFormatService format) {
        return (source, displayName, message, viewer) -> {
            Component rendered = format.render(source, message);
            return rendered != null
                    ? rendered
                    : ChatRenderer.defaultRenderer().render(source, displayName, message, viewer);
        };
    }
}
