package org.glstudio.chat.core.command;

import org.bukkit.command.CommandSender;
import org.glstudio.chat.Chat;
import org.glstudio.nexus.modules.command.Command;
import org.glstudio.nexus.modules.command.CommandManager;

public abstract class ChatPluginCommand extends Command {

    protected final Chat plugin;

    protected ChatPluginCommand(CommandManager manager, Chat plugin, String name, String permission) {
        super(manager, name, permission);
        this.plugin = plugin;
    }

    protected String lang(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    protected void send(CommandSender sender, String path) {
        String message = lang(path);
        if (!message.isBlank()) {
            sendMessage(sender, message);
        }
    }
}
