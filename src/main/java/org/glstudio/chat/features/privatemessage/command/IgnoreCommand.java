package org.glstudio.chat.features.privatemessage.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.Permissions;
import org.glstudio.chat.core.command.ChatPluginCommand;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.Tasks;

import java.util.List;

public class IgnoreCommand extends ChatPluginCommand {
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list");

    public IgnoreCommand(CommandManager manager, Chat plugin) {
        super(manager, plugin, "ignore", Permissions.IGNORE);
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getMessages("ignore_cmd.usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!requirePlayer(sender)) return;

        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> handleAdd((Player) sender, args);
            case "remove" -> handleRemove((Player) sender, args);
            case "list" -> handleList((Player) sender);
            default -> sendUsage(sender);
        }
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            send(player, "ignore_cmd.usage_add");
            return;
        }

        plugin.getPrivateMessageService().addIgnore(player, args[1])
                .thenAccept(result -> new Tasks(plugin).runOnMain(() -> {
                    switch (result) {
                        case ADDED -> sendMessage(player, lang("ignore_cmd.added").replace("%target%", args[1]));
                        case ALREADY_IGNORED ->
                                sendMessage(player, lang("ignore_cmd.already_ignored").replace("%target%", args[1]));
                        case CANNOT_IGNORE_SELF -> send(player, "ignore_cmd.cannot_ignore_self");
                        case PLAYER_NOT_FOUND -> send(player, "global.player_not_found");
                        case NOT_IGNORED, REMOVED -> {
                        }
                    }
                }))
                .exceptionally(error -> {
                    LoggerUtils.logException("IgnoreCommand#handleAdd", error);
                    return null;
                });
    }

    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            send(player, "ignore_cmd.usage_remove");
            return;
        }

        plugin.getPrivateMessageService().removeIgnore(player, args[1])
                .thenAccept(result -> new Tasks(plugin).runOnMain(() -> {
                    switch (result) {
                        case REMOVED -> sendMessage(player, lang("ignore_cmd.removed").replace("%target%", args[1]));
                        case NOT_IGNORED ->
                                sendMessage(player, lang("ignore_cmd.not_ignored").replace("%target%", args[1]));
                        case PLAYER_NOT_FOUND -> send(player, "global.player_not_found");
                        case ADDED, ALREADY_IGNORED, CANNOT_IGNORE_SELF -> {
                        }
                    }
                }))
                .exceptionally(error -> {
                    LoggerUtils.logException("IgnoreCommand#handleRemove", error);
                    return null;
                });
    }

    private void handleList(Player player) {
        List<String> names = plugin.getPrivateMessageService().listIgnored(player);

        if (names.isEmpty()) {
            send(player, "ignore_cmd.list_empty");
            return;
        }

        sendMessage(player, lang("ignore_cmd.list_header").replace("%count%", String.valueOf(names.size())));
        for (String name : names) {
            sendMessage(player, lang("ignore_cmd.list_entry").replace("%target%", name));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabCompletion(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return filterTabCompletion(getOnlinePlayerNames(), args[1]);
        }
        return List.of();
    }
}
