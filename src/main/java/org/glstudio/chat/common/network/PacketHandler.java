package org.glstudio.chat.common.network;

import org.glstudio.chat.common.network.packets.ChatMessagePacket;
import org.glstudio.chat.common.network.packets.NetworkMessagePacket;
import org.glstudio.chat.common.network.packets.PlayerLookupRequestPacket;
import org.glstudio.chat.common.network.packets.PlayerLookupResponsePacket;
import org.glstudio.chat.common.network.packets.PlayerPresencePacket;
import org.glstudio.chat.common.network.packets.PrivateMessageAckPacket;
import org.glstudio.chat.common.network.packets.PrivateMessagePacket;
import org.glstudio.chat.common.storage.redis.RedisService;
import org.glstudio.chat.core.ChatExecutors;
import org.glstudio.chat.core.MultiServerSettings;
import org.glstudio.chat.features.globalchat.GlobalChatService;
import org.glstudio.chat.features.joinquit.JoinQuitService;
import org.glstudio.chat.features.privatemessage.PrivateMessageService;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PacketHandler {
    private final MultiServerSettings settings;
    private final ChatExecutors executors;
    private final RedisService redisService;

    private GlobalChatService globalChatService;
    private JoinQuitService joinQuitService;
    private PrivateMessageService privateMessageService;

    public PacketHandler(MultiServerSettings settings, ChatExecutors executors, RedisService redisService) {
        this.settings = settings;
        this.executors = executors;
        this.redisService = redisService;
    }

    public void setGlobalChatService(GlobalChatService globalChatService) {
        this.globalChatService = globalChatService;
    }

    public void setJoinQuitService(JoinQuitService joinQuitService) {
        this.joinQuitService = joinQuitService;
    }

    public void setPrivateMessageService(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    public CompletableFuture<Void> send(Packet packet) {
        return attemptSend(packet, 0);
    }

    private CompletableFuture<Void> attemptSend(Packet packet, int attemptNo) {
        return CompletableFuture.runAsync(() -> redisService.publishPacket(packet), executors.io())
                .handle((result, error) -> error)
                .thenCompose(error -> {
                    if (error == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    int next = attemptNo + 1;
                    int max = settings.getRetryAttempts();
                    if (next >= max) {
                        LoggerUtils.logError("CRITICAL: failed to publish " + packet.getType()
                                + " after " + max + " attempts: " + error.getMessage());
                        return CompletableFuture.<Void>failedFuture(error);
                    }

                    long backoff = settings.getRetryBackoffMs() * next;
                    return CompletableFuture.supplyAsync(() -> null,
                                    executors.delayed(backoff, TimeUnit.MILLISECONDS))
                            .thenCompose(ignored -> attemptSend(packet, next));
                });
    }

    public void handle(Packet packet) {
        if (packet == null) {
            return;
        }
        try {
            switch (packet.getType()) {
                case CHAT_MESSAGE -> handleChatMessage((ChatMessagePacket) packet);
                case PLAYER_PRESENCE -> handlePlayerPresence((PlayerPresencePacket) packet);
                case NETWORK_MESSAGE -> handleNetworkMessage((NetworkMessagePacket) packet);
                case PRIVATE_MESSAGE -> handlePrivateMessage((PrivateMessagePacket) packet);
                case PRIVATE_MESSAGE_ACK -> handlePrivateMessageAck((PrivateMessageAckPacket) packet);
                case PLAYER_LOOKUP_REQUEST -> handlePlayerLookupRequest((PlayerLookupRequestPacket) packet);
                case PLAYER_LOOKUP_RESPONSE -> handlePlayerLookupResponse((PlayerLookupResponsePacket) packet);
            }
        } catch (Exception e) {
            LoggerUtils.logError("Failed to handle packet " + packet.getType() + ": " + e.getMessage());
        }
    }

    private void handleChatMessage(ChatMessagePacket packet) {
        if (packet.getSourceInstance() != null
                && packet.getSourceInstance().equals(settings.getInstanceId())) {
            return;
        }

        if (globalChatService != null) {
            globalChatService.displayRemote(packet);
        }
    }

    private void handlePlayerPresence(PlayerPresencePacket packet) {
        if (joinQuitService != null) {
            joinQuitService.handleIncomingPresence(packet);
        }
    }

    private void handleNetworkMessage(NetworkMessagePacket packet) {
        if (packet.getSourceInstance() != null
                && packet.getSourceInstance().equals(settings.getInstanceId())) {
            return;
        }

        if (joinQuitService != null) {
            joinQuitService.displayRemote(packet);
        }
    }

    private void handlePrivateMessage(PrivateMessagePacket packet) {
        if (privateMessageService != null) {
            privateMessageService.handleIncomingMessage(packet);
        }
    }

    private void handlePrivateMessageAck(PrivateMessageAckPacket packet) {
        if (privateMessageService != null) {
            privateMessageService.handleIncomingAck(packet);
        }
    }

    private void handlePlayerLookupRequest(PlayerLookupRequestPacket packet) {
        if (privateMessageService != null) {
            privateMessageService.handleIncomingLookupRequest(packet);
        }
    }

    private void handlePlayerLookupResponse(PlayerLookupResponsePacket packet) {
        if (privateMessageService != null) {
            privateMessageService.handleIncomingLookupResponse(packet);
        }
    }
}
