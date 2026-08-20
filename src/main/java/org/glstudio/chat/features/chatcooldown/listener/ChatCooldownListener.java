package org.glstudio.chat.features.chatcooldown.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.glstudio.chat.features.chatcooldown.ChatCooldownService;

public class ChatCooldownListener implements Listener {
    private final ChatCooldownService chatCooldownService;

    public ChatCooldownListener(ChatCooldownService chatCooldownService) {
        this.chatCooldownService = chatCooldownService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (chatCooldownService.handleCommand(e.getPlayer(), e.getMessage())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        chatCooldownService.handleQuit(e.getPlayer());
    }
}
