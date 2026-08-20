package org.glstudio.chat.features.linkblocker;

import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record LinkBlockerSettings(String bypassPermission, Action action, String replacement,
                                  boolean notifyPlayer, String notifyMessage,
                                  boolean notifyStaff, String staffPermission, String staffMessage,
                                  List<String> whitelistedDomains) {
    public enum Action {
        BLOCK,

        REPLACE
    }

    public static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(https?://|www\\.)" +
                    "[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,}" +
                    "([a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]*)?|" +
                    "[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,}/[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+"
    );

    public static LinkBlockerSettings read(ConfigFile config) {
        return new LinkBlockerSettings(
                Configs.string(config, "bypass_permission", "golden.chat.links.bypass"),
                parseAction(Configs.string(config, "action", "block")),
                Configs.string(config, "replacement", "&8[&cLINK REMOVED&8]"),
                Configs.bool(config, "notify_player", true),
                Configs.string(config, "notify_message", "&cLinks are not allowed in chat."),
                Configs.bool(config, "notify_staff", true),
                Configs.string(config, "notify_staff_permission", "golden.chat.links.notify"),
                Configs.string(config, "notify_staff_message", "&8[&6Links&8] &7%player% &8tried to send: &f%message%"),
                Configs.list(config, "whitelisted_domains")
        );
    }

    private static Action parseAction(String raw) {
        try {
            return Action.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LoggerUtils.logWarn("Unknown link-blocker action '" + raw + "'; falling back to BLOCK.");
            return Action.BLOCK;
        }
    }

    static String host(String url) {
        String value = url.toLowerCase(Locale.ROOT);

        int scheme = value.indexOf("://");
        if (scheme >= 0) {
            value = value.substring(scheme + 3);
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == ':' || c == '@') {
                value = value.substring(0, i);
                break;
            }
        }

        return value.startsWith("www.") ? value.substring(4) : value;
    }

    public static boolean isWhitelisted(String url, List<String> whitelist) {
        if (whitelist.isEmpty()) {
            return false;
        }

        String host = host(url);

        for (String entry : whitelist) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String domain = entry.trim().toLowerCase(Locale.ROOT);
            if (domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }
}
