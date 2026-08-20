package org.glstudio.chat.core.chat;

import org.bukkit.entity.Player;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.List;

public class ChatPipeline {
    private final List<ChatProcessor> processors = new ArrayList<>();

    public void register(ChatProcessor processor) {
        if (processor != null) {
            processors.add(processor);
        }
    }

    public ChatResult process(Player player, String message) {
        String current = message;

        for (ChatProcessor processor : processors) {
            ChatResult result;
            try {
                result = processor.process(player, current);
            } catch (Exception e) {
                LoggerUtils.logException(processor.getClass().getSimpleName() + "#process", e);
                continue;
            }

            if (result == null) {
                continue;
            }
            if (result.cancelled()) {
                return result;
            }
            if (result.message() != null) {
                current = result.message();
            }
        }

        return ChatResult.allow(current);
    }

    public void delivered(Player player) {
        for (ChatProcessor processor : processors) {
            try {
                processor.onDelivered(player);
            } catch (Exception e) {
                LoggerUtils.logException(processor.getClass().getSimpleName() + "#onDelivered", e);
            }
        }
    }

    public int size() {
        return processors.size();
    }
}
