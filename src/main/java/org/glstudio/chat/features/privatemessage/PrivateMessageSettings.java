package org.glstudio.chat.features.privatemessage;

import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;

public record PrivateMessageSettings(String senderFormat, String receiverFormat, boolean allowSelfMessage,
                                     String ignoreBypassPermission) {
    public static PrivateMessageSettings read(ConfigFile config) {
        return new PrivateMessageSettings(
                Configs.string(config, "sender_format", "&7[&dYou -> %target%&7] &f%message%"),
                Configs.string(config, "receiver_format", "&7[&d%player% -> You&7] &f%message%"),
                Configs.bool(config, "allow_self_message", false),
                Configs.string(config, "ignore_bypass_permission", "golden.chat.ignore.bypass")
        );
    }
}
