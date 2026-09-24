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

import java.util.Arrays;
import java.util.List;

public class MsgCommand extends ChatPluginCommand {
    public MsgCommand(CommandManager manager, Chat plugin) {
        super(manager, plugin, "msg", Permissions.MSG);
    }

    @Override
    public List<String> aliases() {
        return List.of("tell", "whisper", "w");
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getMessages("msg_cmd.usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return;

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getPrivateMessageService().send((Player) sender, args[0], message)
                .thenAccept(outcome -> new Tasks(plugin).runOnMain(() -> handleOutcome(sender, outcome)))
                .exceptionally(error -> {
                    LoggerUtils.logException("MsgCommand#execute", error);
                    return null;
                });
    }

    private void handleOutcome(CommandSender sender, PrivateMessageService.Outcome outcome) {
        switch (outcome) {
            case PLAYER_NOT_FOUND -> send(sender, "global.player_not_found");
            case CANNOT_MESSAGE_SELF -> send(sender, "msg_cmd.cannot_message_self");
            case BLOCKED_BY_IGNORE -> send(sender, "msg_cmd.blocked_by_target");
            case DELIVERED, NO_REPLY_TARGET -> {
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabCompletion(getOnlinePlayerNames(), args[0]);
        }
        return List.of();
    }
}
