package com.aelion.aero.bukkit;

import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.SelfStatusRequest;
import com.aelion.aero.common.config.AeroConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Periodically pushes live MOTD / player counts to {@code POST /api/aero/v1/self/status}.
 */
public final class SelfStatusReporter {

    private static final long PERIOD_TICKS = 60L; // 3s at 20 TPS

    private final JavaPlugin plugin;
    private final AtomicReference<AeroConfig> configRef;
    private final MotdTracker motdTracker;
    private final Logger logger;
    private final AtomicBoolean pushInFlight = new AtomicBoolean(false);
    private BukkitTask task;
    private String lastMotdSent;
    private int lastPlayersSent = Integer.MIN_VALUE;
    private int lastMaxSent = Integer.MIN_VALUE;

    public SelfStatusReporter(
            JavaPlugin plugin,
            AtomicReference<AeroConfig> configRef,
            MotdTracker motdTracker
    ) {
        this.plugin = plugin;
        this.configRef = configRef;
        this.motdTracker = motdTracker;
        this.logger = plugin.getLogger();
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                schedulePush();
            }
        }, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void schedulePush() {
        AeroConfig config = configRef.get();
        if (config == null || !config.isPanelConfigured()) {
            return;
        }
        if (pushInFlight.get()) {
            return;
        }
        final String motd = motdTracker.motd();
        final int players = Bukkit.getOnlinePlayers().size();
        final int max = Bukkit.getMaxPlayers();
        if (motd.equals(lastMotdSent) && players == lastPlayersSent && max == lastMaxSent) {
            return;
        }
        if (!pushInFlight.compareAndSet(false, true)) {
            return;
        }
        final AeroConfig cfg = config;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    HttpPanelClient client = new HttpPanelClient(cfg);
                    client.postSelfStatus(new SelfStatusRequest(motd, players, max));
                    lastMotdSent = motd;
                    lastPlayersSent = players;
                    lastMaxSent = max;
                } catch (PanelNotConfiguredException ignored) {
                    // nothing to do
                } catch (PanelApiException e) {
                    logger.log(Level.FINE, "self/status push failed: HTTP " + e.statusCode() + " " + e.getMessage());
                } catch (RuntimeException e) {
                    logger.log(Level.FINE, "self/status push failed", e);
                } finally {
                    pushInFlight.set(false);
                }
            }
        });
    }
}
