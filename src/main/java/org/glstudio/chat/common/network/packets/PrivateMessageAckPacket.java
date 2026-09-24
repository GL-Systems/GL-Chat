package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

import java.util.UUID;

@Getter
public class PrivateMessageAckPacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final UUID requestId;
    private final String outcome;
    private final long timestamp;

    public PrivateMessageAckPacket(String sourceServer, String sourceInstance, UUID requestId, String outcome) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.requestId = requestId;
        this.outcome = outcome;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.PRIVATE_MESSAGE_ACK;
    }
}
