package org.glstudio.chat.features.globalchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.common.network.PacketHandler;
import org.glstudio.chat.common.network.packets.ChatMessagePacket;
import org.glstudio.chat.core.MultiServerSettings;
import org.glstudio.chat.core.util.Broadcast;
import org.glstudio.nexus.utils.LoggerUtils;

public class GlobalChatService {
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private final Chat plugin;
    private final MultiServerSettings settings;
    private final PacketHandler packetHandler;

    public GlobalChatService(Chat plugin, MultiServerSettings settings, PacketHandler packetHandler) {
        this.plugin = plugin;
        this.settings = settings;
        this.packetHandler = packetHandler;
    }

    public void publish(Player sender, Component rendered) {
        if (rendered == null) {
            return;
        }

        String json;
        try {
            json = GSON.serialize(rendered);
        } catch (Exception e) {
            LoggerUtils.logError("Failed to serialize chat message from " + sender.getName() + ": " + e.getMessage());
            return;
        }

        ChatMessagePacket packet = new ChatMessagePacket(
                settings.getServerName(),
                settings.getInstanceId(),
                sender.getUniqueId(),
                sender.getName(),
                json
        );

        packetHandler.send(packet).exceptionally(error -> {
            LoggerUtils.logError("Failed to publish chat message from " + sender.getName()
                    + ": " + error.getMessage());
            return null;
        });
    }

    public void displayRemote(ChatMessagePacket packet) {
        Component component;
        try {
            component = GSON.deserialize(packet.getComponentJson());
        } catch (Exception e) {
            LoggerUtils.logError("Discarding malformed chat message from " + packet.getSourceServer()
                    + ": " + e.getMessage());
            return;
        }

        Broadcast.message(plugin, component);
    }
}
