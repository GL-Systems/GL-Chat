package org.glstudio.chat.core;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.glstudio.chat.Chat;
import org.glstudio.chat.common.network.PacketHandler;
import org.glstudio.chat.common.storage.redis.RedisService;
import org.glstudio.chat.core.chat.ChatListener;
import org.glstudio.chat.core.chat.ChatPipeline;
import org.glstudio.chat.features.anticap.AntiCapService;
import org.glstudio.chat.features.chatcontrol.ChatControlService;
import org.glstudio.chat.features.chatcontrol.command.ChatCommand;
import org.glstudio.chat.features.chatcontrol.listener.ChatControlListener;
import org.glstudio.chat.features.chatcooldown.ChatCooldownService;
import org.glstudio.chat.features.chatcooldown.listener.ChatCooldownListener;
import org.glstudio.chat.features.chatfilter.ChatFilterService;
import org.glstudio.chat.features.chatformat.ChatFormatService;
import org.glstudio.chat.features.commandspy.CommandSpyService;
import org.glstudio.chat.features.commandspy.command.CommandSpyCommand;
import org.glstudio.chat.features.commandspy.listener.CommandSpyListener;
import org.glstudio.chat.features.globalchat.GlobalChatService;
import org.glstudio.chat.features.joinquit.JoinQuitService;
import org.glstudio.chat.features.joinquit.listener.JoinQuitListener;
import org.glstudio.chat.features.linkblocker.LinkBlockerService;
import org.glstudio.chat.features.mention.MentionService;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.List;

public class StartupService {
    private final Chat plugin;

    public StartupService(Chat plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        if (!loadConfigs()) return false;
        if (!loadMultiServer()) return false;
        if (!loadFeatures()) return false;

        LoggerUtils.sendEnable(plugin);
        return true;
    }

    private boolean loadConfigs() {
        try {
            plugin.setConfigManager(new ConfigManager(plugin));
            LoggerUtils.logSuccess("Configs loaded.");
            return true;
        } catch (Exception e) {
            LoggerUtils.logException("StartupService#loadConfigs", e);
            return false;
        }
    }

    private boolean loadMultiServer() {
        MultiServerSettings settings = plugin.getConfigManager().getMultiServer();

        if (!settings.isEnabled()) {
            LoggerUtils.logInfo("Multi-server chat disabled.");
            return true;
        }

        try {
            ChatExecutors executors = new ChatExecutors(4);
            plugin.setChatExecutors(executors);

            RedisService redisService = new RedisService(executors, settings);
            redisService.initialize().join();
            plugin.setRedisService(redisService);

            PacketHandler packetHandler = new PacketHandler(settings, executors, redisService);
            plugin.setPacketHandler(packetHandler);

            GlobalChatService globalChatService = new GlobalChatService(plugin, settings, packetHandler);
            plugin.setGlobalChatService(globalChatService);

            packetHandler.setGlobalChatService(globalChatService);
            redisService.setPacketHandler(packetHandler);

            LoggerUtils.logSuccess("Multi-server chat enabled on channel '" + settings.getChannel()
                    + "' as '" + settings.getServerName() + "'.");
            return true;
        } catch (Exception e) {
            LoggerUtils.logError("Multi-server chat could not start; continuing with local chat only.");
            LoggerUtils.logException("StartupService#loadMultiServer", e);

            shutdownPartialMultiServer();
            return true;
        }
    }

    private void shutdownPartialMultiServer() {
        if (plugin.getRedisService() != null) {
            plugin.getRedisService().close().join();
            plugin.setRedisService(null);
        }
        if (plugin.getChatExecutors() != null) {
            plugin.getChatExecutors().shutdown();
            plugin.setChatExecutors(null);
        }
        plugin.setPacketHandler(null);
        plugin.setGlobalChatService(null);
    }

    private boolean loadFeatures() {
        try {
            ConfigManager configManager = plugin.getConfigManager();
            ChatPipeline pipeline = new ChatPipeline();
            plugin.setChatPipeline(pipeline);

            for (Module module : Module.values()) {
                if (!configManager.isModuleEnabled(module)) {
                    continue;
                }
                enable(module);
                LoggerUtils.logSuccess("Module '" + module.getKey() + "' loaded.");
            }

            plugin.setChatControlService(new ChatControlService(plugin));

            pipeline.register(plugin.getChatControlService());
            pipeline.register(plugin.getChatCooldownService());
            pipeline.register(plugin.getChatFilterService());
            pipeline.register(plugin.getLinkBlockerService());
            pipeline.register(plugin.getAntiCapService());
            pipeline.register(plugin.getMentionService());

            int listeners = registerListeners();
            int commands = registerCommands();

            LoggerUtils.logSuccess(pipeline.size() + " chat stages, " + listeners + " listeners and "
                    + commands + " commands registered.");
            return true;
        } catch (Exception e) {
            LoggerUtils.logException("StartupService#loadFeatures", e);
            return false;
        }
    }

    private void enable(Module module) {
        switch (module) {
            case MENTION_SOUNDS -> plugin.setMentionService(new MentionService(plugin));
            case JOIN_QUIT_MESSAGES -> plugin.setJoinQuitService(new JoinQuitService(plugin));
            case ANTI_CAP -> plugin.setAntiCapService(new AntiCapService(plugin));
            case CHAT_FORMAT -> plugin.setChatFormatService(new ChatFormatService(plugin));
            case CHAT_FILTER -> plugin.setChatFilterService(new ChatFilterService(plugin));
            case LINK_BLOCKER -> plugin.setLinkBlockerService(new LinkBlockerService(plugin));
            case COMMAND_SPY -> plugin.setCommandSpyService(new CommandSpyService(plugin));
            case CHAT_COOLDOWN -> plugin.setChatCooldownService(new ChatCooldownService(plugin));
        }
    }

    private int registerListeners() {
        List<Listener> listeners = new ArrayList<>();

        listeners.add(new ChatListener(plugin));
        listeners.add(new ChatControlListener(plugin.getChatControlService()));

        if (plugin.getJoinQuitService() != null) {
            listeners.add(new JoinQuitListener(plugin.getJoinQuitService()));
        }
        if (plugin.getChatCooldownService() != null) {
            listeners.add(new ChatCooldownListener(plugin.getChatCooldownService()));
        }
        if (plugin.getCommandSpyService() != null) {
            listeners.add(new CommandSpyListener(plugin.getCommandSpyService()));
        }

        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }

        return listeners.size();
    }

    private int registerCommands() {
        plugin.getCommandManager().register(new ChatCommand(plugin.getCommandManager(), plugin));

        if (plugin.getCommandSpyService() != null) {
            plugin.getCommandManager().register(new CommandSpyCommand(plugin.getCommandManager(), plugin));
            return 2;
        }

        return 1;
    }
}
