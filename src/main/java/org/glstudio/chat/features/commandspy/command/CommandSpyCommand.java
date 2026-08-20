package org.glstudio.chat.features.commandspy.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.Permissions;
import org.glstudio.chat.core.command.ChatPluginCommand;
import org.glstudio.nexus.modules.command.CommandManager;

import java.util.List;

public class CommandSpyCommand extends ChatPluginCommand {
    public CommandSpyCommand(CommandManager manager, Chat plugin) {
        super(manager, plugin, "commandspy", Permissions.COMMAND_SPY_TOGGLE);
    }

    @Override
    public List<String> aliases() {
        return List.of("cspy");
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getMessages("commandspy_cmd.usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return;

        boolean enabled = plugin.getCommandSpyService().toggleSpy((Player) sender);
        send(sender, enabled ? "commandspy_cmd.enabled" : "commandspy_cmd.disabled");
    }
}
