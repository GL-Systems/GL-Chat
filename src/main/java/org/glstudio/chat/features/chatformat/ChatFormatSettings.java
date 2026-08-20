package org.glstudio.chat.features.chatformat;

import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.glstudio.chat.core.util.Configs;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.TextFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ChatFormatSettings(List<FormatEntry> formats) {
    public record ClickSpec(ClickEvent.Action action, String value) {
    }

    public record FormatPart(String text, List<String> hover, ClickSpec click) {
    }

    public record FormatEntry(String name, int priority, String permission, TextFormat textFormat,
                              List<FormatPart> parts) {
    }

    public static ChatFormatSettings read(ConfigFile config) {
        String ordering = Configs.string(config, "priority-ordering", "HIGHEST_FIRST");
        boolean highestFirst = parseHighestFirst(ordering);

        TextFormat defaultTextFormat = TextFormat.parse(Configs.string(config, "text-format", "LEGACY"),
                TextFormat.LEGACY);

        List<FormatEntry> entries = new ArrayList<>();
        ConfigurationSection formatsSection = config.getConfigurationSection("formats");

        if (formatsSection == null) {
            LoggerUtils.logWarn("chat-format.yml has no 'formats' section; chat will use the default format.");
            return new ChatFormatSettings(List.of());
        }

        for (String key : formatsSection.getKeys(false)) {
            ConfigurationSection section = formatsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            List<FormatPart> parts = readParts(section.getMapList("parts"), key);
            if (parts.isEmpty()) {
                LoggerUtils.logWarn("Format '" + key + "' has no usable parts; skipping it.");
                continue;
            }

            TextFormat textFormat = TextFormat.parse(section.getString("text-format"), defaultTextFormat);

            entries.add(new FormatEntry(key, section.getInt("priority", 0), section.getString("permission"),
                    textFormat, parts));
        }

        Comparator<FormatEntry> byPriority = Comparator.comparingInt(FormatEntry::priority);
        entries.sort(highestFirst ? byPriority.reversed() : byPriority);

        return new ChatFormatSettings(List.copyOf(entries));
    }

    private static boolean parseHighestFirst(String ordering) {
        String value = ordering.trim().toUpperCase(Locale.ROOT);
        if (value.equals("HIGHEST_FIRST")) {
            return true;
        }
        if (value.equals("LOWER_FIRST") || value.equals("LOWEST_FIRST")) {
            return false;
        }

        LoggerUtils.logWarn("Unknown priority-ordering '" + ordering + "'; falling back to HIGHEST_FIRST.");
        return true;
    }

    private static List<FormatPart> readParts(List<Map<?, ?>> raw, String formatName) {
        List<FormatPart> parts = new ArrayList<>();

        for (Map<?, ?> part : raw) {
            String text = asString(part.get("text"));
            if (text == null) {
                LoggerUtils.logWarn("A part of format '" + formatName + "' has no 'text'; skipping it.");
                continue;
            }
            parts.add(new FormatPart(text, asLines(part.get("hover")), asClick(part.get("click"), formatName)));
        }

        return List.copyOf(parts);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<String> asLines(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> lines = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) {
                    lines.add(String.valueOf(item));
                }
            }
            return List.copyOf(lines);
        }
        return List.of(String.valueOf(value).split("\n"));
    }

    private static ClickSpec asClick(Object value, String formatName) {
        if (!(value instanceof Map<?, ?> click)) {
            return null;
        }

        String action = asString(click.get("action"));
        String target = asString(click.get("value"));
        if (action == null || target == null) {
            return null;
        }

        ClickEvent.Action parsed = switch (action.toLowerCase(Locale.ROOT)) {
            case "run_command" -> ClickEvent.Action.RUN_COMMAND;
            case "open_url" -> ClickEvent.Action.OPEN_URL;
            case "copy_to_clipboard" -> ClickEvent.Action.COPY_TO_CLIPBOARD;
            case "suggest_command" -> ClickEvent.Action.SUGGEST_COMMAND;
            default -> {
                LoggerUtils.logWarn("Unknown click action '" + action + "' in format '" + formatName
                        + "'; using suggest_command.");
                yield ClickEvent.Action.SUGGEST_COMMAND;
            }
        };

        return new ClickSpec(parsed, target);
    }
}
