package org.glstudio.chat;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.chat.common.network.PacketHandler;
import org.glstudio.chat.common.storage.redis.RedisService;
import org.glstudio.chat.core.ChatExecutors;
import org.glstudio.chat.core.ConfigManager;
import org.glstudio.chat.core.ShutdownService;
import org.glstudio.chat.core.StartupService;
import org.glstudio.chat.core.chat.ChatPipeline;
import org.glstudio.chat.features.anticap.AntiCapService;
import org.glstudio.chat.features.chatcontrol.ChatControlService;
import org.glstudio.chat.features.chatcooldown.ChatCooldownService;
import org.glstudio.chat.features.chatfilter.ChatFilterService;
import org.glstudio.chat.features.chatformat.ChatFormatService;
import org.glstudio.chat.features.commandspy.CommandSpyService;
import org.glstudio.chat.features.globalchat.GlobalChatService;
import org.glstudio.chat.features.joinquit.JoinQuitService;
import org.glstudio.chat.features.linkblocker.LinkBlockerService;
import org.glstudio.chat.features.mention.MentionService;
import org.glstudio.chat.features.privatemessage.PrivateMessageService;
import org.glstudio.nexus.api.NexusAPI;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.LoggerUtils;

@Getter
@Setter
public final class Chat extends JavaPlugin {

    @Getter
    private static Chat instance;

    private StartupService startupService;
    private ShutdownService shutdownService;

    private ConfigManager configManager;
    private CommandManager commandManager;
    private ChatExecutors chatExecutors;
    private ChatPipeline chatPipeline;

    private RedisService redisService;
    private PacketHandler packetHandler;
    private GlobalChatService globalChatService;

    private MentionService mentionService;
    private JoinQuitService joinQuitService;
    private AntiCapService antiCapService;
    private ChatFormatService chatFormatService;
    private ChatFilterService chatFilterService;
    private LinkBlockerService linkBlockerService;
    private CommandSpyService commandSpyService;
    private ChatCooldownService chatCooldownService;
    private ChatControlService chatControlService;
    private PrivateMessageService privateMessageService;

    @Override
    public void onEnable() {
        instance = this;

        try {
            commandManager = NexusAPI.get().createCommandManager(this);

            startupService = new StartupService(this);
            shutdownService = new ShutdownService(this);

            if (!startupService.initialize()) {
                LoggerUtils.logError("Plugin initialization failed! Disabling...");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (Throwable t) {
            LoggerUtils.logError("Failed to enable GL-Chat: " + t.getMessage());
            LoggerUtils.logException("Chat#onEnable", t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (shutdownService == null) {
            shutdownService = new ShutdownService(this);
        }

        shutdownService.shutdown();
        instance = null;
    }
}