package org.glstudio.chat.features.commandspy;

import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;

import java.util.List;

public record CommandSpySettings(String format, String spyPermission, List<String> blacklisted,
                                 List<String> whitelisted, boolean spyToConsole) {
    public static CommandSpySettings read(ConfigFile config) {
        return new CommandSpySettings(
                Configs.string(config, "format", "&7%player% &8» &7%command%"),
                Configs.string(config, "spy_permission", "golden.chat.commandspy.see"),
                Configs.list(config, "blacklisted_commands"),
                Configs.list(config, "whitelisted_commands"),
                Configs.bool(config, "spy_to_console", true)
        );
    }
}
