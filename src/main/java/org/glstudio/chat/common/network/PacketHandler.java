package org.glstudio.chat.common.network;

import org.glstudio.chat.common.network.packets.ChatMessagePacket;
import org.glstudio.chat.common.storage.redis.RedisService;
import org.glstudio.chat.core.ChatExecutors;
import org.glstudio.chat.core.MultiServerSettings;
import org.glstudio.chat.features.globalchat.GlobalChatService;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PacketHandler {
    private final MultiServerSettings settings;
    private final ChatExecutors executors;
    private final RedisService redisService;

    private GlobalChatService globalChatService;

    public PacketHandler(MultiServerSettings settings, ChatExecutors executors, RedisService redisService) {
        this.settings = settings;
        this.executors = executors;
        this.redisService = redisService;
    }

    public void setGlobalChatService(GlobalChatService globalChatService) {
        this.globalChatService = globalChatService;
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
}
