package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

import java.util.UUID;

@Getter
public class PlayerPresencePacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final UUID playerUuid;
    private final boolean joined;
    private final long timestamp;

    public PlayerPresencePacket(String sourceServer, String sourceInstance, UUID playerUuid, boolean joined) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.playerUuid = playerUuid;
        this.joined = joined;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.PLAYER_PRESENCE;
    }
}
