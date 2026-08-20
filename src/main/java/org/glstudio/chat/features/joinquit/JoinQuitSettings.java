package org.glstudio.chat.features.joinquit;

import org.bukkit.Sound;
import org.glstudio.chat.core.util.Configs;
import org.glstudio.chat.core.util.SoundSetting;
import org.glstudio.nexus.utils.ConfigFile;

public record JoinQuitSettings(Section join, Section quit, boolean firstJoinEnabled, String firstJoinFormat) {
    public record Section(String format, boolean bypassOp, String bypassPermission, SoundSetting sound) {
    }

    public static JoinQuitSettings read(ConfigFile config) {
        return new JoinQuitSettings(
                readSection(config, "join", Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
                readSection(config, "quit", Sound.ENTITY_VILLAGER_NO),
                Configs.bool(config, "join.first_join.enabled", true),
                Configs.string(config, "join.first_join.format",
                        "&8[&a+&8] &b%player_name% &ajoined for the first time!&r")
        );
    }

    private static Section readSection(ConfigFile config, String key, Sound fallbackSound) {
        return new Section(
                Configs.string(config, key + ".format", "&8[&a+&8] %player_name%&r"),
                Configs.bool(config, key + ".bypass_op", false),
                Configs.string(config, key + ".bypass_permission", "golden.chat." + key + ".bypass"),
                SoundSetting.read(config, key + ".sound", Configs.bool(config, key + ".sound.enabled", true),
                        fallbackSound)
        );
    }
}
