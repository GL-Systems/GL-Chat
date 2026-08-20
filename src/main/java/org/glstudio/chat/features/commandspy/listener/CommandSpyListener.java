package org.glstudio.chat.features.commandspy.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.glstudio.chat.features.commandspy.CommandSpyService;

public class CommandSpyListener implements Listener {
    private final CommandSpyService commandSpyService;

    public CommandSpyListener(CommandSpyService commandSpyService) {
        this.commandSpyService = commandSpyService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        commandSpyService.handleCommand(e.getPlayer(), e.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        commandSpyService.handleQuit(e.getPlayer());
    }
}
