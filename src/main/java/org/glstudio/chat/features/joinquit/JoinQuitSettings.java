package org.glstudio.chat.features.joinquit;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.glstudio.chat.core.util.Configs;
import org.glstudio.chat.core.util.SoundSetting;
import org.glstudio.nexus.utils.ConfigFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record JoinQuitSettings(Section join, Section quit, boolean firstJoinEnabled, String firstJoinFormat,
                               List<GroupEntry> groups) {
    public record Section(String format, boolean bypassOp, String bypassPermission, boolean multiServer,
                          SoundSetting sound) {
    }

    public record GroupEntry(String name, int priority, String permission, String joinFormat, String quitFormat,
                             boolean multiServer) {
    }

    public static JoinQuitSettings read(ConfigFile config) {
        Section join = readSection(config, "join", Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        Section quit = readSection(config, "quit", Sound.ENTITY_VILLAGER_NO);

        return new JoinQuitSettings(
                join,
                quit,
                Configs.bool(config, "join.first_join.enabled", true),
                Configs.string(config, "join.first_join.format",
                        "&8[&a+&8] &b%player_name% &ajoined for the first time!&r"),
                readGroups(config, join.format(), quit.format())
        );
    }

    private static Section readSection(ConfigFile config, String key, Sound fallbackSound) {
        return new Section(
                Configs.string(config, key + ".format", "&8[&a+&8] %player_name%&r"),
                Configs.bool(config, key + ".bypass_op", false),
                Configs.string(config, key + ".bypass_permission", "golden.chat." + key + ".bypass"),
                Configs.bool(config, key + ".multi_server", false),
                SoundSetting.read(config, key + ".sound", Configs.bool(config, key + ".sound.enabled", true),
                        fallbackSound)
        );
    }

    private static List<GroupEntry> readGroups(ConfigFile config, String defaultJoinFormat,
                                                String defaultQuitFormat) {
        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
        if (groupsSection == null) {
            return List.of();
        }

        List<GroupEntry> groups = new ArrayList<>();
        for (String key : groupsSection.getKeys(false)) {
            ConfigurationSection section = groupsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String permission = section.getString("permission");
            groups.add(new GroupEntry(
                    key,
                    section.getInt("priority", 0),
                    permission == null || permission.isBlank() ? "group." + key : permission,
                    section.getString("join_format", defaultJoinFormat),
                    section.getString("quit_format", defaultQuitFormat),
                    section.getBoolean("multi_server", false)
            ));
        }

        groups.sort(Comparator.comparingInt(GroupEntry::priority).reversed());
        return List.copyOf(groups);
    }
}
