package org.glstudio.chat.features.chatcontrol.command;

import org.bukkit.command.CommandSender;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.Module;
import org.glstudio.chat.core.Permissions;
import org.glstudio.chat.core.command.ChatPluginCommand;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.List;

public class ChatCommand extends ChatPluginCommand {
    private static final List<String> SUBCOMMANDS =
            List.of("on", "off", "toggle", "status", "clear", "delay", "reload");

    public ChatCommand(CommandManager manager, Chat plugin) {
        super(manager, plugin, "chat", Permissions.ADMIN);
    }

    @Override
    public List<String> aliases() {
        return List.of("glchat", "gchat");
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getMessages("chat_cmd.usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> handleOn(sender);
            case "off" -> handleOff(sender);
            case "toggle" -> handleToggle(sender);
            case "status" -> handleStatus(sender);
            case "clear" -> handleClear(sender);
            case "delay" -> handleDelay(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
    }

    private void handleOn(CommandSender sender) {
        if (plugin.getChatControlService().isChatEnabled()) {
            send(sender, "chat_cmd.already_enabled");
            return;
        }
        plugin.getChatControlService().setChatEnabled(true);
        send(sender, "chat_cmd.enabled");
    }

    private void handleOff(CommandSender sender) {
        if (!plugin.getChatControlService().isChatEnabled()) {
            send(sender, "chat_cmd.already_disabled");
            return;
        }
        plugin.getChatControlService().setChatEnabled(false);
        send(sender, "chat_cmd.disabled");
    }

    private void handleToggle(CommandSender sender) {
        send(sender, plugin.getChatControlService().toggle() ? "chat_cmd.enabled" : "chat_cmd.disabled");
    }

    private void handleStatus(CommandSender sender) {
        boolean enabled = plugin.getChatControlService().isChatEnabled();
        sendMessage(sender, lang("chat_cmd.status").replace("%status%",
                lang(enabled ? "chat_cmd.status_on" : "chat_cmd.status_off")));
    }

    private void handleClear(CommandSender sender) {
        plugin.getChatControlService().clear(sender);
        send(sender, "chat_cmd.cleared_sender");
    }

    private void handleDelay(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isModuleEnabled(Module.CHAT_COOLDOWN)) {
            send(sender, "chat_cmd.delay_module_disabled");
            return;
        }

        if (args.length < 2) {
            send(sender, "chat_cmd.delay_usage");
            return;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            send(sender, "global.invalid_number");
            return;
        }

        if (seconds < 0) {
            send(sender, "global.invalid_number");
            return;
        }

        plugin.getConfigManager().getChatCooldownConfig().set("chat.window_seconds", seconds);
        plugin.getConfigManager().reload();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getConfigManager().getChatCooldownConfig().save());

        sendMessage(sender, lang("chat_cmd.delay_set").replace("%seconds%", String.valueOf(seconds)));
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.getConfigManager().reload();
            send(sender, "chat_cmd.reloaded");
        } catch (Exception e) {
            LoggerUtils.logException("ChatCommand#handleReload", e);
            send(sender, "chat_cmd.reload_failed");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filterTabCompletion(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("delay")) {
            return filterTabCompletion(List.of("1", "3", "5", "10"), args[1]);
        }
        return List.of();
    }
}