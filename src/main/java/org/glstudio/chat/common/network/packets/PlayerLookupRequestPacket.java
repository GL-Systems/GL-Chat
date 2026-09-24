package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

import java.util.UUID;

@Getter
public class PlayerLookupRequestPacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final UUID requestId;
    private final String targetName;
    private final long timestamp;

    public PlayerLookupRequestPacket(String sourceServer, String sourceInstance, UUID requestId, String targetName) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.requestId = requestId;
        this.targetName = targetName;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_LOOKUP_REQUEST;
    }
}
