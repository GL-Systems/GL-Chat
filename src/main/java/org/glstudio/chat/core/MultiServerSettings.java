package org.glstudio.chat.core;

import lombok.Getter;
import org.glstudio.chat.common.storage.redis.RedisChannels;
import org.glstudio.nexus.utils.ConfigFile;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class MultiServerSettings {
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

    private final boolean enabled;
    private final String serverName;

    private final String instanceId;

    private final String channel;
    private final String redisHost;
    private final int redisPort;
    private final String redisPassword;
    private final boolean redisAllowInsecure;
    private final String secret;

    private final int retryAttempts;
    private final long retryBackoffMs;

    public MultiServerSettings(ConfigFile config) {
        this.enabled = config.getBoolean("multi-server.enabled");
        this.serverName = orDefault(resolve(config.getString("multi-server.server-name")), "unnamed-server");
        this.instanceId = UUID.randomUUID().toString();

        this.channel = orDefault(config.getString("multi-server.redis.channel"), RedisChannels.GLOBAL_CHAT);
        this.redisHost = orDefault(resolve(config.getString("multi-server.redis.host")), "localhost");
        this.redisPort = getInt(config, "multi-server.redis.port", 6379);
        this.redisPassword = resolve(config.getString("multi-server.redis.password"));
        this.redisAllowInsecure = config.getBoolean("multi-server.redis.allow-insecure");
        this.secret = resolve(config.getString("multi-server.secret"));

        this.retryAttempts = Math.max(1, getInt(config, "multi-server.retry.attempts", 3));
        this.retryBackoffMs = Math.max(0, getInt(config, "multi-server.retry.backoff-ms", 100));
    }

    private static int getInt(ConfigFile file, String path, int fallback) {
        return file.isExist(path) ? file.getInt(path) : fallback;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    static String resolve(String value) {
        if (value == null || value.indexOf('$') < 0) {
            return value;
        }
        Matcher matcher = ENV_PATTERN.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String env = System.getenv(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(env == null ? "" : env));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}