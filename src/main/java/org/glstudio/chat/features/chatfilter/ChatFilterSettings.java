package org.glstudio.chat.features.chatfilter;

import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record ChatFilterSettings(String bypassPermission, Mode mode, String replacement,
                                 int blockThreshold, boolean notifyPlayer, String notifyMessage,
                                 boolean notifyStaff, String staffPermission, String staffMessage,
                                 List<Pattern> patterns) {
    public enum Mode {
        BLOCK,

        REMOVE,

        REPLACE
    }

    private record LookAlike(char letter, char substitute) {
    }

    private static final List<LookAlike> LOOK_ALIKES = List.of(
            new LookAlike('a', '4'), new LookAlike('a', '@'),
            new LookAlike('e', '3'),
            new LookAlike('i', '1'), new LookAlike('i', '!'),
            new LookAlike('o', '0'),
            new LookAlike('s', '5'), new LookAlike('s', '$'),
            new LookAlike('t', '7'),
            new LookAlike('g', '9')
    );

    public static ChatFilterSettings read(ConfigFile config) {
        boolean caseInsensitive = Configs.bool(config, "case_insensitive", true);
        boolean smartFilter = Configs.bool(config, "smart_filter", true);

        return new ChatFilterSettings(
                Configs.string(config, "bypass_permission", "golden.chat.filter.bypass"),
                parseMode(Configs.string(config, "filter_mode", "REPLACE")),
                Configs.string(config, "replacement", "****"),
                Math.max(1, Configs.integer(config, "block_threshold", 3)),
                Configs.bool(config, "notify_player", true),
                Configs.string(config, "notify_message", "&cYour message contains forbidden words."),
                Configs.bool(config, "notify_staff", true),
                Configs.string(config, "notify_staff_permission", "golden.chat.filter.notify"),
                Configs.string(config, "notify_staff_message", "&8[&cFilter&8] &7%player% &8» &f%message%"),
                compile(Configs.list(config, "filter_words"), caseInsensitive, smartFilter)
        );
    }

    private static Mode parseMode(String raw) {
        try {
            return Mode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LoggerUtils.logWarn("Unknown filter_mode '" + raw + "'; falling back to REPLACE.");
            return Mode.REPLACE;
        }
    }

    public static List<Pattern> compile(List<String> words, boolean caseInsensitive, boolean smartFilter) {
        List<Pattern> compiled = new ArrayList<>();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            try {
                compiled.add(Pattern.compile(regexFor(word, smartFilter),
                        caseInsensitive ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0));
            } catch (PatternSyntaxException e) {
                LoggerUtils.logWarn("Skipping unfilterable word '" + word + "': " + e.getDescription());
            }
        }
        return List.copyOf(compiled);
    }

    static String regexFor(String word, boolean smartFilter) {
        StringBuilder regex = new StringBuilder();

        if (isWordChar(word.charAt(0))) {
            regex.append("\\b");
        }

        for (char c : word.toCharArray()) {
            if (!smartFilter) {
                regex.append(classOf(String.valueOf(c)));
                continue;
            }

            StringBuilder alternatives = new StringBuilder().append(c);
            for (LookAlike lookAlike : LOOK_ALIKES) {
                if (Character.toLowerCase(c) == lookAlike.letter()) {
                    alternatives.append(lookAlike.substitute());
                }
            }
            regex.append(classOf(alternatives.toString()));
        }

        if (isWordChar(word.charAt(word.length() - 1))) {
            regex.append("\\b");
        }

        return regex.toString();
    }

    private static String classOf(String characters) {
        StringBuilder group = new StringBuilder("[");
        for (char c : characters.toCharArray()) {
            if (c == '\\' || c == ']' || c == '[' || c == '^' || c == '-' || c == '&') {
                group.append('\\');
            }
            group.append(c);
        }
        return group.append(']').toString();
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
