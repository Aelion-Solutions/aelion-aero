package com.aelion.aero.bukkit;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.config.AeroConfigLoader;
import java.io.IOException;
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
            new AtomicReference<>(AeroConfig.empty());
    private BukkitFleetService fleetService;
    private BukkitControlHttpServer controlHttpServer;
    private MotdTracker motdTracker;
    private SelfStatusReporter selfStatusReporter;

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
        if (!loadConfigOrShutdown()) {
            return;
        }
        startFleet();
        BukkitAeroCommandExecutor executor = new BukkitAeroCommandExecutor(
                plugin,
                this::config,
                this::reloadAeroConfig,
                fleetService);
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
        if (!loadConfigOrShutdown()) {
            return;
        }
        startFleet();
        plugin.getLogger().info(AeroConstants.NAME + " enabled (fleet bridge registered)");
    }

    private void startFleet() {
        fleetService = new BukkitFleetService(plugin, configRef);
        fleetService.start();
        plugin.getServer().getServicesManager().register(
                AeroFleetService.class,
                fleetService,
                plugin,
                ServicePriority.Normal);
        motdTracker = new MotdTracker(plugin);
        selfStatusReporter = new SelfStatusReporter(plugin, configRef, motdTracker);
        selfStatusReporter.start();
    }

    /**
     * Loads config and starts the control server. If the control API is required
     * (control.enabled=true) but fails to bind, shuts the Bukkit server down so
     * the cloud daemon does not keep routing players into a backend that cannot
     * be drained via {@code POST /v1/shutdown}. Returns {@code true} on success,
     * {@code false} when server shutdown was requested.
     */
    private boolean loadConfigOrShutdown() {
        try {
            reloadAeroConfig();
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Aero control API is required (control.enabled=true) but failed to start: "
                            + e.getMessage()
                            + " — shutting down server to avoid running without a daemon shutdown listener.",
                    e);
            plugin.getServer().shutdown();
            return false;
        }
    }

    public void disable() {
        if (selfStatusReporter != null) {
            selfStatusReporter.stop();
            selfStatusReporter = null;
        }
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

    public void reloadAeroConfig() throws IOException {
        try {
            configRef.set(AeroConfigLoader.loadDataDirectory(
                    plugin.getDataFolder().toPath(),
                    plugin.getClass().getClassLoader(),
                    msg -> plugin.getLogger().info(msg)));
            // Keep Bukkit's FileConfiguration in sync for any callers of getConfig().
            plugin.reloadConfig();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load Aero config: " + e.getMessage(), e);
            configRef.set(BukkitConfigBridge.fromBukkitMerged(
                    plugin.getDataFolder().toPath(),
                    plugin.getConfig()));
        }
        restartControlServer();
    }

    /**
     * (Re)starts the loopback control HTTP server. Rethrows the underlying
     * {@link IOException} when {@code control.enabled=true} and the listener
     * cannot bind, so callers can surface the failure to admins ({@code /aes
     * reload}) or shut the server down on initial enable rather than silently
     * running without a shutdown endpoint the daemon depends on.
     */
    private void restartControlServer() throws IOException {
        if (controlHttpServer == null) {
            controlHttpServer = new BukkitControlHttpServer(plugin, configRef, this::fleetService);
        }
        try {
            controlHttpServer.start(config().control());
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Control API failed to start: " + e.getMessage(), e);
            throw e;
        }
    }
}
