package com.aelion.aero.velocity;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.config.AeroConfigLoader;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;

/**
 * Velocity entry point for Aelion Aero.
 */
@Plugin(
        id = "aelionaero",
        name = AeroConstants.NAME,
        version = AeroVersion.VERSION,
        authors = {"Aelion Solutions"},
        description = "Connects this Velocity proxy to Aelion Cloud",
        url = "https://github.com/Aelion-Solutions/aelion-aero"
)
public final class AeroVelocityPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private AeroConfig aeroConfig = new AeroConfig("", "", "", AeroConfig.ControlConfig.disabled());
    private BackendRegistryService registryService;
    private ControlHttpServer controlHttpServer;

    @Inject
    public AeroVelocityPlugin(
            ProxyServer proxy,
            Logger logger,
            @DataDirectory Path dataDirectory
    ) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ensureDefaultConfig();
        registryService = new BackendRegistryService(proxy, logger);
        controlHttpServer = new ControlHttpServer(logger, registryService);
        try {
            reloadAeroConfig();
        } catch (Exception e) {
            logger.error("Failed to load Aero config", e);
        }
        AeroVelocityCommand.register(this);
        logger.info("{} enabled on {}", AeroConstants.NAME, proxy.getVersion().getName());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (controlHttpServer != null) {
            controlHttpServer.stop();
        }
        logger.info("{} disabled", AeroConstants.NAME);
    }

    ProxyServer proxy() {
        return proxy;
    }

    public AeroConfig aeroConfig() {
        return aeroConfig;
    }

    public void reloadAeroConfig() throws IOException {
        Path configPath = dataDirectory.resolve("config.yml");
        ensureDefaultConfig();
        aeroConfig = AeroConfigLoader.loadYaml(configPath);
        if (controlHttpServer != null) {
            try {
                controlHttpServer.start(aeroConfig.control());
            } catch (IOException e) {
                logger.error("Control API failed to start: {}", e.getMessage());
                throw e;
            }
        }
    }

    private void ensureDefaultConfig() {
        try {
            Files.createDirectories(dataDirectory);
            Path configPath = dataDirectory.resolve("config.yml");
            if (Files.notExists(configPath)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (in == null) {
                        logger.warn("Bundled config.yml missing from jar");
                        return;
                    }
                    Files.copy(in, configPath);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to prepare config.yml", e);
        }
    }
}
