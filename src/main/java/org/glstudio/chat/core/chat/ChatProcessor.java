package org.glstudio.chat.core.chat;

import org.bukkit.entity.Player;

public interface ChatProcessor {
    ChatResult process(Player player, String message);

    default void onDelivered(Player player) {
    }
}
