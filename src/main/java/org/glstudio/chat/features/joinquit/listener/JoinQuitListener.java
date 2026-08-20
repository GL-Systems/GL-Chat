package org.glstudio.chat.features.joinquit.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.glstudio.chat.features.joinquit.JoinQuitService;

public class JoinQuitListener implements Listener {
    private final JoinQuitService joinQuitService;

    public JoinQuitListener(JoinQuitService joinQuitService) {
        this.joinQuitService = joinQuitService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        joinQuitService.handleJoin(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        joinQuitService.handleQuit(e);
    }
}
