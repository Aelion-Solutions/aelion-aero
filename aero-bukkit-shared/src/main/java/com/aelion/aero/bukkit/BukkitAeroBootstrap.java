package com.aelion.aero.bukkit;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.config.AeroConfig;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Shared enable/disable wiring for all Bukkit/Paper Aero backend bands.
 */
public final class BukkitAeroBootstrap {

    private final JavaPlugin plugin;
    private final AtomicReference<AeroConfig> configRef =
            new AtomicReference<>(new AeroConfig("", "", "", AeroConfig.ControlConfig.disabled()));
    private BukkitFleetService fleetService;
    private BukkitControlHttpServer controlHttpServer;

    public BukkitAeroBootstrap(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public AtomicReference<AeroConfig> configRef() {
        return configRef;
    }

    public AeroConfig config() {
        return configRef.get();
    }

    public BukkitFleetService fleetService() {
        return fleetService;
    }

    public void enableWithClassicCommands() {
        plugin.saveDefaultConfig();
        reloadAeroConfig();
        fleetService = new BukkitFleetService(plugin, configRef);
        fleetService.start();
        plugin.getServer().getServicesManager().register(
                AeroFleetService.class,
                fleetService,
                plugin,
                ServicePriority.Normal);

        BukkitAeroCommandExecutor executor = new BukkitAeroCommandExecutor(
                plugin,
                this::config,
                this::reloadAeroConfig);
        BukkitAeroCommandExecutor.register(
                plugin,
                executor,
                AeroConstants.COMMAND_BACKEND_PRIMARY);

        plugin.getLogger().info(AeroConstants.NAME + " enabled (fleet bridge registered)");
    }

    /**
     * Fleet + config only — caller registers Brigadier (or other) commands.
     */
    public void enableFleetOnly() {
        plugin.saveDefaultConfig();
        reloadAeroConfig();
        fleetService = new BukkitFleetService(plugin, configRef);
        fleetService.start();
        plugin.getServer().getServicesManager().register(
                AeroFleetService.class,
                fleetService,
                plugin,
                ServicePriority.Normal);
        plugin.getLogger().info(AeroConstants.NAME + " enabled (fleet bridge registered)");
    }

    public void disable() {
        if (controlHttpServer != null) {
            controlHttpServer.stop();
            controlHttpServer = null;
        }
        plugin.getServer().getServicesManager().unregisterAll(plugin);
        if (fleetService != null) {
            fleetService.stop();
        }
        plugin.getLogger().info(AeroConstants.NAME + " disabled");
    }

    public void reloadAeroConfig() {
        plugin.reloadConfig();
        configRef.set(BukkitConfigBridge.fromBukkit(plugin.getConfig()));
        restartControlServer();
    }

    private void restartControlServer() {
        if (controlHttpServer == null) {
            controlHttpServer = new BukkitControlHttpServer(plugin);
        }
        try {
            controlHttpServer.start(config().control());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Control API failed to start: " + e.getMessage(), e);
        }
    }
}
