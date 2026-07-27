package com.aelion.aero.bungee;

import com.aelion.aero.common.fleet.FleetNotifyService;
import java.util.UUID;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Quit cleanup + chat delivery for {@link FleetNotifyService} (push via control API).
 */
final class BungeeFleetNotifyBridge implements Listener {

    private final AeroBungeePlugin plugin;
    private final FleetNotifyService notifyService;

    BungeeFleetNotifyBridge(AeroBungeePlugin plugin, FleetNotifyService notifyService) {
        this.plugin = plugin;
        this.notifyService = notifyService;
    }

    void start() {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    void stop() {
        notifyService.clear();
        plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    void deliver(UUID playerId, String legacyLine) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.sendMessage(TextComponent.fromLegacy(legacyLine == null ? "" : legacyLine));
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        notifyService.removeSubscriber(event.getPlayer().getUniqueId());
    }
}
