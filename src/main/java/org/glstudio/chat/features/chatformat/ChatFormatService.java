package org.glstudio.chat.features.chatformat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.util.Placeholders;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.TextFormat;

import java.util.List;
import java.util.regex.Pattern;

public class ChatFormatService {
    private static final Pattern MESSAGE_TOKEN = Pattern.compile(Pattern.quote("%message%"));
    private static final String MESSAGE_SLOT = "message";

    private final Chat plugin;

    public ChatFormatService(Chat plugin) {
        this.plugin = plugin;
    }

    private ChatFormatSettings settings() {
        return plugin.getConfigManager().getChatFormat();
    }

    public Component render(Player sender, Component message) {
        ChatFormatSettings.FormatEntry format = resolveFormat(sender);
        if (format == null) {
            return null;
        }

        Component result = Component.empty();

        for (ChatFormatSettings.FormatPart part : format.parts()) {
            Component component = compose(sender, part.text(), message, format.textFormat());

            if (!part.hover().isEmpty()) {
                component = component.hoverEvent(HoverEvent.showText(
                        buildHover(sender, part.hover(), message, format.textFormat())));
            }

            ChatFormatSettings.ClickSpec click = part.click();
            if (click != null) {
                String value = Placeholders.apply(sender, click.value()).replace("%message%", "");
                component = component.clickEvent(clickEvent(click.action(), value));
            }

            result = result.append(component);
        }

        return result;
    }

    private static ClickEvent clickEvent(ClickEvent.Action action, String value) {
        return switch (action) {
            case RUN_COMMAND -> ClickEvent.runCommand(value);
            case OPEN_URL -> ClickEvent.openUrl(value);
            case COPY_TO_CLIPBOARD -> ClickEvent.copyToClipboard(value);
            default -> ClickEvent.suggestCommand(value);
        };
    }

    private ChatFormatSettings.FormatEntry resolveFormat(Player player) {
        for (ChatFormatSettings.FormatEntry entry : settings().formats()) {
            String permission = entry.permission();
            if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                LoggerUtils.logDebug("ChatFormat", "Using format '" + entry.name() + "' for " + player.getName());
                return entry;
            }
        }

        LoggerUtils.logDebug("ChatFormat", "No format matched " + player.getName()
                + "; falling back to the server default.");
        return null;
    }

    private Component buildHover(Player sender, List<String> lines, Component message, TextFormat format) {
        Component hover = Component.empty();

        for (int i = 0; i < lines.size(); i++) {
            hover = hover.append(compose(sender, lines.get(i), message, format));
            if (i < lines.size() - 1) {
                hover = hover.append(Component.newline());
            }
        }

        return hover;
    }

    private Component compose(Player sender, String text, Component message, TextFormat format) {
        if (format == TextFormat.MINIMESSAGE) {
            String template = text.replace("%message%", "<" + MESSAGE_SLOT + ">");
            return CC.miniMessage(Placeholders.apply(sender, template), MESSAGE_SLOT, message);
        }

        String[] pieces = MESSAGE_TOKEN.split(text, -1);

        Component result = CC.component(Placeholders.apply(sender, pieces[0]));
        for (int i = 1; i < pieces.length; i++) {
            result = result.append(message).append(CC.component(Placeholders.apply(sender, pieces[i])));
        }

        return result;
    }
}
