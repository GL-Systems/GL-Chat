package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

@Getter
public class NetworkMessagePacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final String componentJson;
    private final long timestamp;

    public NetworkMessagePacket(String sourceServer, String sourceInstance, String componentJson) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.componentJson = componentJson;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.NETWORK_MESSAGE;
    }
}
