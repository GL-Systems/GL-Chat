package org.glstudio.chat.features.chatcontrol.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.glstudio.chat.features.chatcontrol.ChatControlService;

public class ChatControlListener implements Listener {
    private final ChatControlService chatControlService;

    public ChatControlListener(ChatControlService chatControlService) {
        this.chatControlService = chatControlService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (chatControlService.isCommandBlocked(e.getPlayer(), e.getMessage())) {
            e.setCancelled(true);
        }
    }
}