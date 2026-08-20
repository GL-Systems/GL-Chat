package org.glstudio.chat.core;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.util.Configs;
import org.glstudio.chat.features.anticap.AntiCapSettings;
import org.glstudio.chat.features.chatcooldown.ChatCooldownSettings;
import org.glstudio.chat.features.chatfilter.ChatFilterSettings;
import org.glstudio.chat.features.chatformat.ChatFormatSettings;
import org.glstudio.chat.features.commandspy.CommandSpySettings;
import org.glstudio.chat.features.joinquit.JoinQuitSettings;
import org.glstudio.chat.features.linkblocker.LinkBlockerSettings;
import org.glstudio.chat.features.mention.MentionSettings;
import org.glstudio.nexus.modules.command.Command;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Getter
public class ConfigManager {
    private static final String DEFAULT_LANGUAGE = "en";

    private final Chat plugin;

    private final ConfigFile mainConfig;
    private final ConfigFile langConfig;

    private final ConfigFile mentionConfig;
    private final ConfigFile joinQuitConfig;
    private final ConfigFile antiCapConfig;
    private final ConfigFile chatFormatConfig;
    private final ConfigFile chatFilterConfig;
    private final ConfigFile linkBlockerConfig;
    private final ConfigFile commandSpyConfig;
    private final ConfigFile chatCooldownConfig;

    private boolean debug;
    private List<String> blockedCommandsWhenChatOff;

    private MultiServerSettings multiServer;

    private MentionSettings mention;
    private JoinQuitSettings joinQuit;
    private AntiCapSettings antiCap;
    private ChatFormatSettings chatFormat;
    private ChatFilterSettings chatFilter;
    private LinkBlockerSettings linkBlocker;
    private CommandSpySettings commandSpy;
    private ChatCooldownSettings chatCooldown;

    public ConfigManager(Chat plugin) {
        this.plugin = plugin;

        this.mainConfig = new ConfigFile(plugin, "config.yml");
        this.langConfig = new ConfigFile(plugin, "lang", "lang_" + resolveLanguage(plugin, mainConfig) + ".yml");

        this.mentionConfig = new ConfigFile(plugin, "modules", "mention.yml");
        this.joinQuitConfig = new ConfigFile(plugin, "modules", "join-quit-messages.yml");
        this.antiCapConfig = new ConfigFile(plugin, "modules", "anti-cap.yml");
        this.chatFormatConfig = new ConfigFile(plugin, "modules", "chat-format.yml");
        this.chatFilterConfig = new ConfigFile(plugin, "modules", "chat-filter.yml");
        this.linkBlockerConfig = new ConfigFile(plugin, "modules", "link-blocker.yml");
        this.commandSpyConfig = new ConfigFile(plugin, "modules", "command-spy.yml");
        this.chatCooldownConfig = new ConfigFile(plugin, "modules", "chat-cooldown.yml");

        load();
    }

    public void reload() {
        mainConfig.reload();
        langConfig.reload();
        mentionConfig.reload();
        joinQuitConfig.reload();
        antiCapConfig.reload();
        chatFormatConfig.reload();
        chatFilterConfig.reload();
        linkBlockerConfig.reload();
        commandSpyConfig.reload();
        chatCooldownConfig.reload();

        load();
    }

    private void load() {
        this.debug = Configs.bool(mainConfig, "debug", false);
        LoggerUtils.setDebugEnabled(debug);

        this.blockedCommandsWhenChatOff = Configs.list(mainConfig, "blacklisted-commands-disabled-chat");

        this.multiServer = new MultiServerSettings(mainConfig);

        this.mention = MentionSettings.read(mentionConfig);
        this.joinQuit = JoinQuitSettings.read(joinQuitConfig);
        this.antiCap = AntiCapSettings.read(antiCapConfig);
        this.chatFormat = ChatFormatSettings.read(chatFormatConfig);
        this.chatFilter = ChatFilterSettings.read(chatFilterConfig);
        this.linkBlocker = LinkBlockerSettings.read(linkBlockerConfig);
        this.commandSpy = CommandSpySettings.read(commandSpyConfig);
        this.chatCooldown = ChatCooldownSettings.read(chatCooldownConfig);

        Command.setNoPermissionMessage(getMessage("global.no_permission"));
        Command.setPlayersOnlyMessage(getMessage("global.only_players"));

        validate();
    }

    private void validate() {
        Set<String> known = Module.keys();
        ConfigurationSection modules = mainConfig.getConfigurationSection("modules");
        if (modules != null) {
            for (String key : modules.getKeys(false)) {
                if (!known.contains(key)) {
                    LoggerUtils.logWarn("config.yml lists an unknown module '" + key + "'; it does nothing.");
                }
            }
        }

        if (langConfig.getKeys(false).isEmpty()) {
            LoggerUtils.logWarn("The selected language file is empty; every message will be blank.");
        }

        if (isModuleEnabled(Module.CHAT_FORMAT) && chatFormat.formats().isEmpty()) {
            LoggerUtils.logWarn("chat-format is enabled but no usable format is defined.");
        }

        if (multiServer.isEnabled() && multiServer.getChannel().isBlank()) {
            LoggerUtils.logWarn("MULTI_SERVER is enabled but REDIS.CHANNEL is empty.");
        }
    }

    private static String resolveLanguage(Chat plugin, ConfigFile config) {
        String configured = Configs.string(config, "lang", DEFAULT_LANGUAGE).trim().toLowerCase(Locale.ROOT);
        String resource = "lang/lang_" + configured + ".yml";

        boolean bundled = plugin.getResource(resource) != null;
        boolean onDisk = new File(plugin.getDataFolder(), resource).isFile();

        if (bundled || onDisk) {
            return configured;
        }

        LoggerUtils.logWarn("No language file for lang: " + configured + "; falling back to '"
                + DEFAULT_LANGUAGE + "'.");
        return DEFAULT_LANGUAGE;
    }

    public boolean isModuleEnabled(Module module) {
        return Configs.bool(mainConfig, "modules." + module.getKey(), true);
    }

    public String getMessage(String path) {
        if (!langConfig.isExist(path)) {
            LoggerUtils.logWarn("Missing language key: " + path);
            return "";
        }
        String value = langConfig.getString(path);
        return value == null ? "" : value;
    }

    public List<String> getMessages(String path) {
        if (!langConfig.isExist(path)) {
            LoggerUtils.logWarn("Missing language key: " + path);
            return List.of();
        }
        return langConfig.getStringList(path);
    }
}
