package org.glstudio.chat.features.joinquit;

import org.glstudio.nexus.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.glstudio.chat.Chat;
import org.glstudio.chat.common.network.PacketHandler;
import org.glstudio.chat.common.network.packets.NetworkMessagePacket;
import org.glstudio.chat.common.network.packets.PlayerPresencePacket;
import org.glstudio.chat.core.MultiServerSettings;
import org.glstudio.chat.core.util.Broadcast;
import org.glstudio.chat.core.util.Placeholders;
import org.glstudio.chat.core.util.SoundSetting;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.Tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JoinQuitService {
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private static final long PRESENCE_GRACE_MS = 8_000L;

    private static final long JOIN_DECISION_DELAY_TICKS = 20L;

    private static final long QUIT_DECISION_DELAY_TICKS = 100L;

    private final Chat plugin;

    private final Map<UUID, Long> recentJoinHints = new ConcurrentHashMap<>();
    private final Map<UUID, Long> recentQuitHints = new ConcurrentHashMap<>();

    public JoinQuitService(Chat plugin) {
        this.plugin = plugin;
        new Tasks(plugin).executeScheduledAsync(1200L, this::purgeStaleHints);
    }

    private JoinQuitSettings settings() {
        return plugin.getConfigManager().getJoinQuit();
    }

    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        JoinQuitSettings settings = settings();

        event.joinMessage(null);

        if (canBypass(player, settings.join())) {
            return;
        }

        boolean firstJoin = !player.hasPlayedBefore();
        if (firstJoin && settings.firstJoinEnabled()) {
            announce(player, settings.firstJoinFormat());
            settings.join().sound().playToAll(plugin);
            return;
        }

        JoinQuitSettings.GroupEntry group = resolveGroup(player, settings.groups());
        String format = group != null ? group.joinFormat() : settings.join().format();
        boolean multiServer = group != null ? group.multiServer() : settings.join().multiServer();
        SoundSetting sound = settings.join().sound();

        if (!multiServer || !networkAvailable()) {
            announce(player, format);
            sound.playToAll(plugin);
            return;
        }

        UUID uuid = player.getUniqueId();
        publishPresenceHint(uuid, true);

        new Tasks(plugin).executeLater(JOIN_DECISION_DELAY_TICKS, () -> {
            if (!player.isOnline() || consumeRecentHint(recentQuitHints, uuid)) {
                return;
            }

            Component message = renderMessage(player, format);
            Broadcast.message(plugin, message);
            sound.playToAll(plugin);
            publishAnnounce(message);
        });
    }

    public void handleQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        JoinQuitSettings settings = settings();

        event.quitMessage(null);

        if (canBypass(player, settings.quit())) {
            return;
        }

        JoinQuitSettings.GroupEntry group = resolveGroup(player, settings.groups());
        String format = group != null ? group.quitFormat() : settings.quit().format();
        boolean multiServer = group != null ? group.multiServer() : settings.quit().multiServer();
        SoundSetting sound = settings.quit().sound();

        if (!multiServer || !networkAvailable()) {
            announce(player, format);
            sound.playToAll(plugin);
            return;
        }

        UUID uuid = player.getUniqueId();
        Component message = renderMessage(player, format);
        publishPresenceHint(uuid, false);

        new Tasks(plugin).executeLater(QUIT_DECISION_DELAY_TICKS, () -> {
            if (consumeRecentHint(recentJoinHints, uuid)) {
                return;
            }

            Broadcast.message(plugin, message);
            sound.playToAll(plugin);
            publishAnnounce(message);
        });
    }

    public void handleIncomingPresence(PlayerPresencePacket packet) {
        Map<UUID, Long> target = packet.isJoined() ? recentJoinHints : recentQuitHints;
        target.put(packet.getPlayerUuid(), System.currentTimeMillis());
    }

    public void displayRemote(NetworkMessagePacket packet) {
        Component component;
        try {
            component = GSON.deserialize(packet.getComponentJson());
        } catch (Exception e) {
            LoggerUtils.logError("Discarding malformed join/quit announcement from "
                    + packet.getSourceServer() + ": " + e.getMessage());
            return;
        }

        Broadcast.message(plugin, component);
    }

    private boolean networkAvailable() {
        return plugin.getConfigManager().getMultiServer().isEnabled() && plugin.getPacketHandler() != null;
    }

    private void publishPresenceHint(UUID uuid, boolean joined) {
        PacketHandler handler = plugin.getPacketHandler();
        if (handler == null) {
            return;
        }

        MultiServerSettings multiServer = plugin.getConfigManager().getMultiServer();
        PlayerPresencePacket packet = new PlayerPresencePacket(
                multiServer.getServerName(), multiServer.getInstanceId(), uuid, joined);

        handler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish presence hint: " + error.getMessage());
            return null;
        });
    }

    private void publishAnnounce(Component message) {
        PacketHandler handler = plugin.getPacketHandler();
        if (handler == null) {
            return;
        }

        String json;
        try {
            json = GSON.serialize(message);
        } catch (Exception e) {
            LoggerUtils.logError("Failed to serialize join/quit announcement: " + e.getMessage());
            return;
        }

        MultiServerSettings multiServer = plugin.getConfigManager().getMultiServer();
        NetworkMessagePacket packet = new NetworkMessagePacket(
                multiServer.getServerName(), multiServer.getInstanceId(), json);

        handler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish join/quit announcement: " + error.getMessage());
            return null;
        });
    }

    private boolean consumeRecentHint(Map<UUID, Long> hints, UUID uuid) {
        Long seenAt = hints.remove(uuid);
        return seenAt != null && (System.currentTimeMillis() - seenAt) <= PRESENCE_GRACE_MS;
    }

    private void purgeStaleHints() {
        long cutoff = System.currentTimeMillis() - PRESENCE_GRACE_MS;
        recentJoinHints.values().removeIf(seenAt -> seenAt < cutoff);
        recentQuitHints.values().removeIf(seenAt -> seenAt < cutoff);
    }

    private JoinQuitSettings.GroupEntry resolveGroup(Player player, List<JoinQuitSettings.GroupEntry> groups) {
        for (JoinQuitSettings.GroupEntry group : groups) {
            if (player.hasPermission(group.permission())) {
                return group;
            }
        }
        return null;
    }

    private Component renderMessage(Player player, String format) {
        return CC.component(Placeholders.apply(player, format));
    }

    private void announce(Player player, String format) {
        Broadcast.message(plugin, renderMessage(player, format));
    }

    private boolean canBypass(Player player, JoinQuitSettings.Section section) {
        if (section.bypassOp() && player.isOp()) {
            return true;
        }
        String permission = section.bypassPermission();
        return permission != null && !permission.isEmpty() && player.hasPermission(permission);
    }
}
