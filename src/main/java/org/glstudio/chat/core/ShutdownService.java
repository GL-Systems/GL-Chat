package org.glstudio.chat.core;

import org.bukkit.event.HandlerList;
import org.glstudio.chat.Chat;
import org.glstudio.nexus.utils.LoggerUtils;

public class ShutdownService {
    private final Chat plugin;

    public ShutdownService(Chat plugin) {
        this.plugin = plugin;
    }

    public void shutdown() {
        try {
            HandlerList.unregisterAll(plugin);

            if (plugin.getCommandManager() != null) {
                plugin.getCommandManager().unregisterAll();
            }

            if (plugin.getChatCooldownService() != null) {
                plugin.getChatCooldownService().clear();
            }
            if (plugin.getCommandSpyService() != null) {
                plugin.getCommandSpyService().clear();
            }

            if (plugin.getRedisService() != null) {
                plugin.getRedisService().close().join();
            }

            if (plugin.getChatExecutors() != null) {
                plugin.getChatExecutors().shutdown();
            }

            LoggerUtils.logSuccess("Services shut down cleanly.");
        } catch (Exception e) {
            LoggerUtils.logException("ShutdownService#shutdown", e);
        }

        LoggerUtils.sendDisable(plugin);
    }
}
