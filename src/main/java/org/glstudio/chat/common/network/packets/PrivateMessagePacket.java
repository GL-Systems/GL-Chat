package org.glstudio.chat.common.network.packets;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketType;

import java.util.UUID;

@Getter
public class PrivateMessagePacket extends Packet {
    private final String sourceServer;
    private final String sourceInstance;
    private final UUID requestId;
    private final UUID senderUuid;
    private final String senderName;
    private final String targetName;
    private final String rawMessage;
    private final boolean allowColor;
    private final boolean ignoreBypass;
    private final long timestamp;

    public PrivateMessagePacket(String sourceServer, String sourceInstance, UUID requestId, UUID senderUuid,
                                String senderName, String targetName, String rawMessage, boolean allowColor,
                                boolean ignoreBypass) {
        this.sourceServer = sourceServer;
        this.sourceInstance = sourceInstance;
        this.requestId = requestId;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetName = targetName;
        this.rawMessage = rawMessage;
        this.allowColor = allowColor;
        this.ignoreBypass = ignoreBypass;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public PacketType getType() {
        return PacketType.PRIVATE_MESSAGE;
    }
}
