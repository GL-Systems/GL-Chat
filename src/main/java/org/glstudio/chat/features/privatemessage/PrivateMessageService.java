package org.glstudio.chat.features.privatemessage;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.common.network.PacketHandler;
import org.glstudio.chat.common.network.packets.PlayerLookupRequestPacket;
import org.glstudio.chat.common.network.packets.PlayerLookupResponsePacket;
import org.glstudio.chat.common.network.packets.PrivateMessageAckPacket;
import org.glstudio.chat.common.network.packets.PrivateMessagePacket;
import org.glstudio.chat.core.MultiServerSettings;
import org.glstudio.chat.core.Permissions;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class PrivateMessageService {
    private static final Pattern MESSAGE_TOKEN = Pattern.compile(Pattern.quote("%message%"));

    private static final long NETWORK_TIMEOUT_TICKS = 40L;

    public enum Outcome {
        DELIVERED, PLAYER_NOT_FOUND, CANNOT_MESSAGE_SELF, BLOCKED_BY_IGNORE, NO_REPLY_TARGET
    }

    public enum IgnoreChange {
        ADDED, REMOVED, ALREADY_IGNORED, NOT_IGNORED, CANNOT_IGNORE_SELF, PLAYER_NOT_FOUND
    }

    private record ResolvedPlayer(UUID uuid, String name) {
    }

    private final Chat plugin;
    private final ConfigFile ignoresConfig;

    private final Map<UUID, String> replyTarget = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> ignored = new ConcurrentHashMap<>();

    private final Map<UUID, CompletableFuture<Outcome>> pendingMessages = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<ResolvedPlayer>> pendingLookups = new ConcurrentHashMap<>();

    public PrivateMessageService(Chat plugin) {
        this.plugin = plugin;
        this.ignoresConfig = new ConfigFile(plugin, "data", "ignores.yml");
        loadIgnores();
    }

    private PrivateMessageSettings settings() {
        return plugin.getConfigManager().getPrivateMessage();
    }

    private boolean networkAvailable() {
        return plugin.getConfigManager().getMultiServer().isEnabled() && plugin.getPacketHandler() != null;
    }

    public CompletableFuture<Outcome> send(Player sender, String targetName, String rawMessage) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            return CompletableFuture.completedFuture(sendLocal(sender, target, rawMessage));
        }

        if (!networkAvailable()) {
            return CompletableFuture.completedFuture(Outcome.PLAYER_NOT_FOUND);
        }

        return sendNetwork(sender, targetName, rawMessage);
    }

    public CompletableFuture<Outcome> reply(Player sender, String rawMessage) {
        String targetName = replyTarget.get(sender.getUniqueId());
        if (targetName == null) {
            return CompletableFuture.completedFuture(Outcome.NO_REPLY_TARGET);
        }
        return send(sender, targetName, rawMessage);
    }

    private Outcome sendLocal(Player sender, Player target, String rawMessage) {
        if (!settings().allowSelfMessage() && target.getUniqueId().equals(sender.getUniqueId())) {
            return Outcome.CANNOT_MESSAGE_SELF;
        }
        if (blockedByIgnore(sender, target)) {
            return Outcome.BLOCKED_BY_IGNORE;
        }

        deliver(sender, target, rawMessage);
        return Outcome.DELIVERED;
    }

    private CompletableFuture<Outcome> sendNetwork(Player sender, String targetName, String rawMessage) {
        PacketHandler handler = plugin.getPacketHandler();
        if (handler == null) {
            return CompletableFuture.completedFuture(Outcome.PLAYER_NOT_FOUND);
        }

        UUID requestId = UUID.randomUUID();
        CompletableFuture<Outcome> future = new CompletableFuture<>();
        pendingMessages.put(requestId, future);

        boolean allowColor = sender.hasPermission(Permissions.COLOR);
        boolean ignoreBypass = sender.hasPermission(settings().ignoreBypassPermission());
        MultiServerSettings multiServer = plugin.getConfigManager().getMultiServer();

        PrivateMessagePacket packet = new PrivateMessagePacket(multiServer.getServerName(),
                multiServer.getInstanceId(), requestId, sender.getUniqueId(), sender.getName(), targetName,
                rawMessage, allowColor, ignoreBypass);

        handler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish private message: " + error.getMessage());
            completeMessage(requestId, Outcome.PLAYER_NOT_FOUND);
            return null;
        });

        new Tasks(plugin).executeLater(NETWORK_TIMEOUT_TICKS, () -> completeMessage(requestId, Outcome.PLAYER_NOT_FOUND));

        return future.thenApply(outcome -> {
            if (outcome == Outcome.DELIVERED) {
                new Tasks(plugin).runOnMain(() -> {
                    if (!sender.isOnline()) {
                        return;
                    }
                    Component message = CC.componentOfUserInput(rawMessage, allowColor);
                    sender.sendMessage(compose(settings().senderFormat(), sender.getName(), targetName, message));
                    replyTarget.put(sender.getUniqueId(), targetName);
                });
            }
            return outcome;
        });
    }

    public void handleIncomingMessage(PrivateMessagePacket packet) {
        new Tasks(plugin).runOnMain(() -> {
            Player target = Bukkit.getPlayerExact(packet.getTargetName());
            if (target == null) {
                return;
            }

            Outcome outcome;
            if (isIgnoringUuid(target, packet.getSenderUuid()) && !packet.isIgnoreBypass()) {
                outcome = Outcome.BLOCKED_BY_IGNORE;
            } else {
                PrivateMessageSettings settings = settings();
                Component message = CC.componentOfUserInput(packet.getRawMessage(), packet.isAllowColor());

                target.sendMessage(compose(settings.receiverFormat(), packet.getSenderName(), target.getName(),
                        message));
                replyTarget.put(target.getUniqueId(), packet.getSenderName());
                outcome = Outcome.DELIVERED;
            }

            publishAck(packet.getRequestId(), outcome);
        });
    }

    public void handleIncomingAck(PrivateMessageAckPacket packet) {
        Outcome outcome;
        try {
            outcome = Outcome.valueOf(packet.getOutcome());
        } catch (IllegalArgumentException e) {
            outcome = Outcome.PLAYER_NOT_FOUND;
        }
        completeMessage(packet.getRequestId(), outcome);
    }

    public void handleIncomingLookupRequest(PlayerLookupRequestPacket packet) {
        new Tasks(plugin).runOnMain(() -> {
            Player found = Bukkit.getPlayerExact(packet.getTargetName());
            if (found != null) {
                publishLookupResponse(packet.getRequestId(), found.getUniqueId(), found.getName());
            }
        });
    }

    public void handleIncomingLookupResponse(PlayerLookupResponsePacket packet) {
        completeLookup(packet.getRequestId(), new ResolvedPlayer(packet.getPlayerUuid(), packet.getPlayerName()));
    }

    private void completeMessage(UUID requestId, Outcome outcome) {
        CompletableFuture<Outcome> pending = pendingMessages.remove(requestId);
        if (pending != null) {
            pending.complete(outcome);
        }
    }

    private void publishAck(UUID requestId, Outcome outcome) {
        PacketHandler handler = plugin.getPacketHandler();
        if (handler == null) {
            return;
        }

        MultiServerSettings multiServer = plugin.getConfigManager().getMultiServer();
        PrivateMessageAckPacket packet = new PrivateMessageAckPacket(multiServer.getServerName(),
                multiServer.getInstanceId(), requestId, outcome.name());

        handler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish private message ack: " + error.getMessage());
            return null;
        });
    }

    private void publishLookupResponse(UUID requestId, UUID playerUuid, String playerName) {
        PacketHandler handler = plugin.getPacketHandler();
        if (handler == null) {
            return;
        }

        MultiServerSettings multiServer = plugin.getConfigManager().getMultiServer();
        PlayerLookupResponsePacket packet = new PlayerLookupResponsePacket(multiServer.getServerName(),
                multiServer.getInstanceId(), requestId, playerUuid, playerName);

        handler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish player lookup response: " + error.getMessage());
            return null;
        });
    }

    private boolean blockedByIgnore(Player sender, Player target) {
        return isIgnoring(target, sender) && !sender.hasPermission(settings().ignoreBypassPermission());
    }

    private void deliver(Player sender, Player target, String rawMessage) {
        PrivateMessageSettings settings = settings();
        Component message = CC.componentOfUserInput(rawMessage, sender.hasPermission(Permissions.COLOR));

        sender.sendMessage(compose(settings.senderFormat(), sender.getName(), target.getName(), message));
        target.sendMessage(compose(settings.receiverFormat(), sender.getName(), target.getName(), message));

        replyTarget.put(sender.getUniqueId(), target.getName());
        replyTarget.put(target.getUniqueId(), sender.getName());
    }

    private Component compose(String format, String senderName, String targetName, Component message) {
        String template = format.replace("%player%", senderName).replace("%target%", targetName);
        String[] pieces = MESSAGE_TOKEN.split(template, -1);

        Component result = CC.component(pieces[0]);
        for (int i = 1; i < pieces.length; i++) {
            result = result.append(message).append(CC.component(pieces[i]));
        }
        return result;
    }

    public boolean isIgnoring(Player owner, Player target) {
        return isIgnoringUuid(owner, target.getUniqueId());
    }

    private boolean isIgnoringUuid(Player owner, UUID otherUuid) {
        Set<UUID> set = ignored.get(owner.getUniqueId());
        return set != null && set.contains(otherUuid);
    }

    public CompletableFuture<IgnoreChange> addIgnore(Player player, String targetName) {
        return resolve(targetName).thenApply(resolved -> {
            if (resolved == null) {
                return IgnoreChange.PLAYER_NOT_FOUND;
            }
            if (resolved.uuid().equals(player.getUniqueId())) {
                return IgnoreChange.CANNOT_IGNORE_SELF;
            }

            Set<UUID> set = ignored.computeIfAbsent(player.getUniqueId(), id -> ConcurrentHashMap.newKeySet());
            if (!set.add(resolved.uuid())) {
                return IgnoreChange.ALREADY_IGNORED;
            }

            persist(player.getUniqueId());
            return IgnoreChange.ADDED;
        });
    }

    public CompletableFuture<IgnoreChange> removeIgnore(Player player, String targetName) {
        return resolve(targetName).thenApply(resolved -> {
            if (resolved == null) {
                return IgnoreChange.PLAYER_NOT_FOUND;
            }

            Set<UUID> set = ignored.get(player.getUniqueId());
            if (set == null || !set.remove(resolved.uuid())) {
                return IgnoreChange.NOT_IGNORED;
            }

            persist(player.getUniqueId());
            return IgnoreChange.REMOVED;
        });
    }

    public List<String> listIgnored(Player player) {
        Set<UUID> set = ignored.get(player.getUniqueId());
        if (set == null || set.isEmpty()) {
            return List.of();
        }

        List<String> names = new ArrayList<>(set.size());
        for (UUID id : set) {
            String name = Bukkit.getOfflinePlayer(id).getName();
            names.add(name != null ? name : id.toString());
        }

        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    @SuppressWarnings("deprecation")
    private CompletableFuture<ResolvedPlayer> resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return CompletableFuture.completedFuture(new ResolvedPlayer(online.getUniqueId(), online.getName()));
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) {
            String resolvedName = offline.getName();
            return CompletableFuture.completedFuture(
                    new ResolvedPlayer(offline.getUniqueId(), resolvedName != null ? resolvedName : name));
        }

        if (!networkAvailable()) {
            return CompletableFuture.completedFuture(null);
        }

        return resolveNetwork(name);
    }

    private CompletableFuture<ResolvedPlayer> resolveNetwork(String name) {
        PacketHandler handler = plugin.getPacketHandler();
        if (handler == null) {
            return CompletableFuture.completedFuture(null);
        }

        UUID requestId = UUID.randomUUID();
        CompletableFuture<ResolvedPlayer> future = new CompletableFuture<>();
        pendingLookups.put(requestId, future);

        MultiServerSettings multiServer = plugin.getConfigManager().getMultiServer();
        PlayerLookupRequestPacket packet = new PlayerLookupRequestPacket(multiServer.getServerName(),
                multiServer.getInstanceId(), requestId, name);

        handler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish player lookup: " + error.getMessage());
            completeLookup(requestId, null);
            return null;
        });

        new Tasks(plugin).executeLater(NETWORK_TIMEOUT_TICKS, () -> completeLookup(requestId, null));

        return future;
    }

    private void completeLookup(UUID requestId, ResolvedPlayer resolved) {
        CompletableFuture<ResolvedPlayer> pending = pendingLookups.remove(requestId);
        if (pending != null) {
            pending.complete(resolved);
        }
    }

    private void persist(UUID owner) {
        Set<UUID> set = ignored.get(owner);
        List<String> values = set == null || set.isEmpty()
                ? null
                : set.stream().map(UUID::toString).toList();

        ignoresConfig.set(owner.toString(), values);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> ignoresConfig.save());
    }

    private void loadIgnores() {
        for (String key : ignoresConfig.getKeys(false)) {
            UUID owner = parseUuid(key);
            if (owner == null) {
                continue;
            }

            Set<UUID> set = ConcurrentHashMap.newKeySet();
            for (String raw : ignoresConfig.getStringList(key)) {
                UUID id = parseUuid(raw);
                if (id != null) {
                    set.add(id);
                }
            }

            if (!set.isEmpty()) {
                ignored.put(owner, set);
            }
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
