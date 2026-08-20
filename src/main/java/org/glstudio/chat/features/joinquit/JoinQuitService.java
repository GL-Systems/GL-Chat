package org.glstudio.chat.features.joinquit;

import org.glstudio.nexus.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.glstudio.chat.Chat;
import org.glstudio.chat.core.util.Broadcast;
import org.glstudio.chat.core.util.Placeholders;

public class JoinQuitService {
    private final Chat plugin;

    public JoinQuitService(Chat plugin) {
        this.plugin = plugin;
    }

    private JoinQuitSettings settings() {
        return plugin.getConfigManager().getJoinQuit();
    }

    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        JoinQuitSettings settings = settings();

        event.joinMessage(null);

        if (canBypass(player, settings.join())) {
            return;
        }

        boolean firstJoin = !player.hasPlayedBefore();
        String format = firstJoin && settings.firstJoinEnabled()
                ? settings.firstJoinFormat()
                : settings.join().format();

        announce(player, format);
        settings.join().sound().playToAll(plugin);
    }

    public void handleQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        JoinQuitSettings settings = settings();

        event.quitMessage(null);

        if (canBypass(player, settings.quit())) {
            return;
        }

        announce(player, settings.quit().format());
        settings.quit().sound().playToAll(plugin);
    }

    private void announce(Player player, String format) {
        Component message = CC.component(Placeholders.apply(player, format));
        Broadcast.message(plugin, message);
    }

    private boolean canBypass(Player player, JoinQuitSettings.Section section) {
        if (section.bypassOp() && player.isOp()) {
            return true;
        }
        String permission = section.bypassPermission();
        return permission != null && !permission.isEmpty() && player.hasPermission(permission);
    }
}
