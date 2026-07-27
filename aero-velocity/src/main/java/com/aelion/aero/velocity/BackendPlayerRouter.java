package com.aelion.aero.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Routes new logins using the live Aero registry (Velocity try-list is not mutable at runtime).
 */
final class BackendPlayerRouter {

    private final BackendRegistryService registryService;
    private final Logger logger;

    BackendPlayerRouter(BackendRegistryService registryService, Logger logger) {
        this.registryService = registryService;
        this.logger = logger;
    }

    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Optional<RegisteredServer> target = registryService.resolveInitialServer(null);
        if (target.isEmpty()) {
            logger.debug("No Aero try/lobby target for {}; leaving Velocity default",
                    event.getPlayer().getUsername());
            return;
        }
        event.setInitialServer(target.get());
    }
}
