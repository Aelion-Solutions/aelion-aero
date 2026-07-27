package com.aelion.aero.velocity;

import com.aelion.aero.common.fleet.FleetNotifyService;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Quit cleanup + chat delivery for {@link FleetNotifyService} (push via control API).
 */
final class VelocityFleetNotifyBridge {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();

    private final AeroVelocityPlugin plugin;
    private final ProxyServer proxy;
    private final FleetNotifyService notifyService;

    VelocityFleetNotifyBridge(
            AeroVelocityPlugin plugin,
            ProxyServer proxy,
            FleetNotifyService notifyService
    ) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.notifyService = notifyService;
    }

    void start() {
        proxy.getEventManager().register(plugin, this);
    }

    void stop() {
        notifyService.clear();
        proxy.getEventManager().unregisterListener(plugin, this);
    }

    void deliver(UUID playerId, String legacyLine) {
        Optional<Player> player = proxy.getPlayer(playerId);
        if (!player.isPresent()) {
            return;
        }
        player.get().sendMessage(LEGACY.deserialize(legacyLine == null ? "" : legacyLine));
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        notifyService.removeSubscriber(event.getPlayer().getUniqueId());
    }
}
