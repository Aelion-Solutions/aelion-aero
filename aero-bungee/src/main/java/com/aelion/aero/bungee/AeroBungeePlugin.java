package com.aelion.aero.bungee;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.config.AeroConfigLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord / Waterfall entry point for Aelion Aero.
 */
public final class AeroBungeePlugin extends Plugin {

    private AeroConfig aeroConfig = new AeroConfig("", "", "", AeroConfig.ControlConfig.disabled());
    private BackendRegistryService registryService;
    private ControlHttpServer controlHttpServer;

    @Override
    public void onEnable() {
        ensureDefaultConfig();
        registryService = new BackendRegistryService(getProxy(), getLogger());
        controlHttpServer = new ControlHttpServer(getLogger(), registryService);
        try {
            reloadAeroConfig();
        } catch (Exception e) {
            getLogger().severe("Failed to load Aero config: " + e.getMessage());
        }
        getProxy().getPluginManager().registerListener(this, new BackendPlayerRouter(registryService, getLogger()));
        getProxy().getPluginManager().registerCommand(this, new AeroBungeeCommand(this));
        getLogger().info(AeroConstants.NAME + " " + AeroVersion.VERSION + " enabled on BungeeCord/Waterfall");
    }

    @Override
    public void onDisable() {
        if (controlHttpServer != null) {
            controlHttpServer.stop();
        }
        getLogger().info(AeroConstants.NAME + " disabled");
    }

    public AeroConfig aeroConfig() {
        return aeroConfig;
    }

    public void reloadAeroConfig() throws IOException {
        Path configPath = getDataFolder().toPath().resolve("config.yml");
        ensureDefaultConfig();
        aeroConfig = AeroConfigLoader.loadYaml(configPath);
        if (controlHttpServer != null) {
            controlHttpServer.start(aeroConfig.control());
        }
    }

    private void ensureDefaultConfig() {
        try {
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("Could not create data folder");
            }
            Path configPath = getDataFolder().toPath().resolve("config.yml");
            if (Files.notExists(configPath)) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in == null) {
                        getLogger().warning("Bundled config.yml missing from jar");
                        return;
                    }
                    Files.copy(in, configPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            getLogger().severe("Failed to prepare config.yml: " + e.getMessage());
        }
    }
}
