package org.glstudio.chat.features.chatcooldown;

import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.chat.ChatProcessor;
import org.glstudio.chat.core.chat.ChatResult;
import org.glstudio.chat.core.util.CommandNames;
import org.glstudio.nexus.utils.CC;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCooldownService implements ChatProcessor {
    private final Chat plugin;

    private final Map<UUID, Deque<Long>> chatTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> commandTimestamps = new ConcurrentHashMap<>();

    public ChatCooldownService(Chat plugin) {
        this.plugin = plugin;
    }

    private ChatCooldownSettings settings() {
        return plugin.getConfigManager().getChatCooldown();
    }

    @Override
    public ChatResult process(Player player, String message) {
        ChatCooldownSettings settings = settings();

        if (!settings.chatEnabled()) return ChatResult.allow(message);
        if (player.hasPermission(settings.bypassPermission())) return ChatResult.allow(message);

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = chatTimestamps.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());

        synchronized (timestamps) {
            prune(timestamps, now, settings.windowMillis());

            if (timestamps.size() < settings.maxMessages()) {
                return ChatResult.allow(message);
            }

            long oldest = timestamps.peekFirst();
            long remaining = settings.windowMillis() - (now - oldest);
            long seconds = Math.max(1, (long) Math.ceil(remaining / 1000.0));

            player.sendMessage(CC.t(settings.chatMessage().replace("%seconds%", String.valueOf(seconds))));
            return ChatResult.cancel();
        }
    }

    @Override
    public void onDelivered(Player player) {
        ChatCooldownSettings settings = settings();

        if (!settings.chatEnabled()) return;
        if (player.hasPermission(settings.bypassPermission())) return;

        long now = System.currentTimeMillis();
        Deque<Long> timestamps = chatTimestamps.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());

        synchronized (timestamps) {
            prune(timestamps, now, settings.windowMillis());
            timestamps.addLast(now);
        }
    }

    private void prune(Deque<Long> timestamps, long now, long windowMillis) {
        Iterator<Long> iterator = timestamps.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next() > windowMillis) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    public boolean handleCommand(Player player, String command) {
        ChatCooldownSettings settings = settings();

        if (!settings.commandsEnabled()) return false;
        if (player.hasPermission(settings.bypassPermission())) return false;

        if (!settings.watchedCommands().isEmpty()
                && !CommandNames.matches(command, settings.watchedCommands())) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long last = commandTimestamps.get(player.getUniqueId());

        if (last != null) {
            long elapsed = now - last;
            if (elapsed < settings.commandCooldownMillis()) {
                long seconds = Math.max(1, (long) Math.ceil((settings.commandCooldownMillis() - elapsed) / 1000.0));
                player.sendMessage(CC.t(settings.commandMessage().replace("%seconds%", String.valueOf(seconds))));
                return true;
            }
        }

        commandTimestamps.put(player.getUniqueId(), now);
        return false;
    }

    public void handleQuit(Player player) {
        chatTimestamps.remove(player.getUniqueId());
        commandTimestamps.remove(player.getUniqueId());
    }

    public void clear() {
        chatTimestamps.clear();
        commandTimestamps.clear();
    }
}
