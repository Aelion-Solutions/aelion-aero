package com.aelion.aero.bungee;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.config.AeroConfigLoader;
import java.io.IOException;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord / Waterfall entry point for Aelion Aero.
 */
public final class AeroBungeePlugin extends Plugin {

    private AeroConfig aeroConfig = AeroConfig.empty();
    private BackendRegistryService registryService;
    private ControlHttpServer controlHttpServer;

    @Override
    public void onEnable() {
        registryService = new BackendRegistryService(getProxy(), getLogger());
        controlHttpServer = new ControlHttpServer(getLogger(), getProxy(), this, registryService);
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

    BackendRegistryService registryService() {
        return registryService;
    }

    public void reloadAeroConfig() throws IOException {
        aeroConfig = AeroConfigLoader.loadDataDirectory(
                getDataFolder().toPath(),
                getClass().getClassLoader(),
                msg -> getLogger().info(msg));
        if (controlHttpServer != null) {
            controlHttpServer.start(aeroConfig.control());
        }
    }
}
