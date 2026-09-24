package org.glstudio.chat.common.storage.redis;

import lombok.Getter;
import org.glstudio.chat.common.network.Packet;
import org.glstudio.chat.common.network.PacketCodec;
import org.glstudio.chat.common.network.PacketHandler;
import org.glstudio.chat.core.ChatExecutors;
import org.glstudio.chat.core.MultiServerSettings;
import org.glstudio.nexus.utils.LoggerUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;

@Getter
public class RedisService {
    private static final long MAX_BACKOFF_MS = 60_000L;

    private final ChatExecutors executors;
    private final MultiServerSettings settings;
    private JedisPool jedisPool;
    private PacketHandler packetHandler;
    private Thread subscriberThread;
    private volatile JedisPubSub subscription;
    private volatile boolean running;

    private String chatChannel;
    private String secret;

    public RedisService(ChatExecutors executors, MultiServerSettings settings) {
        this.executors = executors;
        this.settings = settings;
    }

    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                String host = settings.getRedisHost();
                int port = settings.getRedisPort();
                String password = settings.getRedisPassword();
                boolean allowInsecure = settings.isRedisAllowInsecure();

                if ((password == null || password.isEmpty()) && !allowInsecure) {
                    throw new IllegalStateException("Redis has no password. Set MULTI_SERVER.REDIS.PASSWORD, or "
                            + "multi-server.REDIS.ALLOW_INSECURE: true to override (not recommended).");
                }

                this.chatChannel = settings.getChannel();
                this.secret = settings.getSecret();

                if (secret == null || secret.isEmpty()) {
                    LoggerUtils.logWarn("multi-server.SECRET is empty; chat packets are unsigned and unauthenticated");
                }

                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(10);
                poolConfig.setMaxIdle(5);
                poolConfig.setMinIdle(1);

                if (password == null || password.isEmpty()) {
                    this.jedisPool = new JedisPool(poolConfig, host, port);
                } else {
                    this.jedisPool = new JedisPool(poolConfig, host, port, 2000, password);
                }

                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.ping();
                }

                this.running = true;
                startSubscriber();

                LoggerUtils.logSuccess("Redis connection established");
            } catch (Exception e) {
                LoggerUtils.logError("Failed to connect to Redis: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, executors.io());
    }

    public void setPacketHandler(PacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    private void startSubscriber() {
        subscriberThread = new Thread(() -> {
            long backoff = 5000L;
            while (running) {
                try (Jedis jedis = jedisPool.getResource()) {
                    LoggerUtils.logInfo("Redis subscriber connected");
                    backoff = 5000L;
                    this.subscription = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            handleMessage(channel, message);
                        }
                    };
                    jedis.subscribe(subscription, chatChannel);
                } catch (Exception e) {
                    if (!running) {
                        break;
                    }
                    LoggerUtils.logWarn("Redis subscriber disconnected: " + e.getMessage()
                            + " (retrying in " + (backoff / 1000) + "s)");
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                }
            }
        }, "GL-Chat-Redis-Subscriber");

        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    private void handleMessage(String channel, String message) {
        try {
            Packet packet = PacketCodec.decode(message, secret);
            if (packetHandler != null) {
                packetHandler.handle(packet);
            }
        } catch (SecurityException e) {
            LoggerUtils.logWarn("Rejected Redis message on " + channel + ": " + e.getMessage());
        } catch (Exception e) {
            LoggerUtils.logError("Failed to handle Redis message on " + channel + ": " + e.getMessage());
        }
    }

    public void publishPacket(Packet packet) {
        String channel = switch (packet.getType()) {
            case CHAT_MESSAGE, PLAYER_PRESENCE, NETWORK_MESSAGE, PRIVATE_MESSAGE, PRIVATE_MESSAGE_ACK,
                 PLAYER_LOOKUP_REQUEST, PLAYER_LOOKUP_RESPONSE -> chatChannel;
        };

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channel, PacketCodec.encode(packet, secret));
        }
    }

    public boolean isRunning() {
        return running;
    }

    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            running = false;

            JedisPubSub sub = this.subscription;
            if (sub != null && sub.isSubscribed()) {
                try {
                    sub.unsubscribe();
                } catch (Exception ignored) {
                }
            }

            if (subscriberThread != null) {
                subscriberThread.interrupt();
            }

            if (jedisPool != null && !jedisPool.isClosed()) {
                jedisPool.close();
            }
        });
    }
}
