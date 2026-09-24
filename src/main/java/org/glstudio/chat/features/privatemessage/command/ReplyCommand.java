package org.glstudio.chat.features.privatemessage.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.Permissions;
import org.glstudio.chat.core.command.ChatPluginCommand;
import org.glstudio.chat.features.privatemessage.PrivateMessageService;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.Tasks;

import java.util.List;

public class ReplyCommand extends ChatPluginCommand {
    public ReplyCommand(CommandManager manager, Chat plugin) {
        super(manager, plugin, "reply", Permissions.MSG);
    }

    @Override
    public List<String> aliases() {
        return List.of("r");
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getMessages("reply_cmd.usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return;

        if (args.length < 1) {
            sendUsage(sender);
            return;
        }

        String message = String.join(" ", args);
        plugin.getPrivateMessageService().reply((Player) sender, message)
                .thenAccept(outcome -> new Tasks(plugin).runOnMain(() -> handleOutcome(sender, outcome)))
                .exceptionally(error -> {
                    LoggerUtils.logException("ReplyCommand#execute", error);
                    return null;
                });
    }

    private void handleOutcome(CommandSender sender, PrivateMessageService.Outcome outcome) {
        switch (outcome) {
            case NO_REPLY_TARGET -> send(sender, "reply_cmd.no_target");
            case PLAYER_NOT_FOUND -> send(sender, "global.player_not_found");
            case BLOCKED_BY_IGNORE -> send(sender, "msg_cmd.blocked_by_target");
            case DELIVERED, CANNOT_MESSAGE_SELF -> {
            }
        }
    }
}
