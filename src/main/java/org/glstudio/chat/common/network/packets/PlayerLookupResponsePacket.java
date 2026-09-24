package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

import java.util.UUID;

@Getter
public class PlayerLookupResponsePacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final UUID requestId;
    private final UUID playerUuid;
    private final String playerName;
    private final long timestamp;

    public PlayerLookupResponsePacket(String sourceServer, String sourceInstance, UUID requestId, UUID playerUuid,
                                      String playerName) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.requestId = requestId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_LOOKUP_RESPONSE;
    }
}
