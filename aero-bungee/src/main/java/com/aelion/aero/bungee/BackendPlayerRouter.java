package com.aelion.aero.bungee;

import java.util.Optional;
import java.util.logging.Logger;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

/**
 * Routes proxy joins using the live Aero registry (listener priorities stay empty on disk).
 */
final class BackendPlayerRouter implements Listener {

    private final BackendRegistryService registryService;
    private final Logger logger;

    BackendPlayerRouter(BackendRegistryService registryService, Logger logger) {
        this.registryService = registryService;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event) {
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }
        Optional<ServerInfo> target = registryService.resolveInitialServer(null);
        if (target.isEmpty()) {
            logger.fine("No Aero try/lobby target for " + event.getPlayer().getName());
            return;
        }
        event.setTarget(target.get());
    }
}
