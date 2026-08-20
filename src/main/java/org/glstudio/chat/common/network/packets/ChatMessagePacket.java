package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

import java.util.UUID;

@Getter
public class ChatMessagePacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final UUID senderUuid;
    private final String senderName;
    private final String componentJson;
    private final long timestamp;

    public ChatMessagePacket(String sourceServer, String sourceInstance, UUID senderUuid,
                             String senderName, String componentJson) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.componentJson = componentJson;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.CHAT_MESSAGE;
    }
}
