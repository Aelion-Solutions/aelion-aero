package com.aelion.aero.bungee;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelClient;
import com.aelion.aero.common.api.PanelHttp;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.config.AeroConfigLoader;
import com.aelion.aero.common.fleet.FleetNotifyService;
import java.io.IOException;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * BungeeCord / Waterfall entry point for Aelion Aero.
 */
public final class AeroBungeePlugin extends Plugin {

    private AeroConfig aeroConfig = AeroConfig.empty();
    private PanelHttp panelHttp;
    private BackendRegistryService registryService;
    private ControlHttpServer controlHttpServer;
    private FleetNotifyService notifyService;
    private BungeeFleetNotifyBridge notifyBridge;

    @Override
    public void onEnable() {
        registryService = new BackendRegistryService(getProxy(), getLogger());
        notifyService = new FleetNotifyService(this::aeroConfig, this::deliverNotify);
        notifyBridge = new BungeeFleetNotifyBridge(this, notifyService);
        notifyBridge.start();
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
        if (notifyBridge != null) {
            notifyBridge.stop();
            notifyBridge = null;
        }
        notifyService = null;
        if (controlHttpServer != null) {
            controlHttpServer.stop();
        }
        if (panelHttp != null) {
            panelHttp.close();
            panelHttp = null;
        }
        getLogger().info(AeroConstants.NAME + " disabled");
    }

    public AeroConfig aeroConfig() {
        return aeroConfig;
    }

    public PanelClient panelClient() {
        AeroConfig cfg = aeroConfig;
        if (panelHttp == null) {
            return new HttpPanelClient(cfg);
        }
        return panelHttp.panelClient(cfg);
    }

    BackendRegistryService registryService() {
        return registryService;
    }

    FleetNotifyService notifyService() {
        return notifyService;
    }

    private void deliverNotify(java.util.UUID playerId, String legacyLine) {
        if (notifyBridge != null) {
            notifyBridge.deliver(playerId, legacyLine);
        }
    }

    public void reloadAeroConfig() throws IOException {
        aeroConfig = AeroConfigLoader.loadDataDirectory(
                getDataFolder().toPath(),
                getClass().getClassLoader(),
                msg -> getLogger().info(msg));
        ensurePanelHttp();
        if (controlHttpServer != null) {
            controlHttpServer.start(aeroConfig.control());
        }
    }

    private void ensurePanelHttp() {
        boolean insecure = aeroConfig.panelInsecureSsl();
        if (panelHttp != null && panelHttp.insecureSsl() == insecure) {
            return;
        }
        if (panelHttp != null) {
            panelHttp.close();
        }
        panelHttp = new PanelHttp(insecure);
    }
}
