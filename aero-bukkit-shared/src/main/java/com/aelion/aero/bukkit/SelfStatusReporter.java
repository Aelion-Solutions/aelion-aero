package com.aelion.aero.bukkit;

import com.aelion.aero.common.AeroIo;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelClient;
import com.aelion.aero.common.api.PanelHttp;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.SelfStatusRequest;
import com.aelion.aero.common.config.AeroConfig;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically pushes live MOTD / player counts to {@code POST /api/aero/v1/self/status}.
 *
 * <p>Runs on {@link AeroIo}, not the Bukkit primary thread. Player counts come from
 * {@link OnlineRoster}.
 */
public final class SelfStatusReporter {

    private static final long PERIOD_MS = 3_000L;

    private final AtomicReference<AeroConfig> configRef;
    private final MotdTracker motdTracker;
    private final OnlineRoster roster;
    private final Supplier<PanelHttp> panelHttp;
    private final AeroIo aeroIo;
    private final Logger logger;
    private final AtomicBoolean pushInFlight = new AtomicBoolean(false);
    private ScheduledFuture<?> task;
    private String lastMotdSent;
    private int lastPlayersSent = Integer.MIN_VALUE;
    private int lastMaxSent = Integer.MIN_VALUE;

    SelfStatusReporter(
            AtomicReference<AeroConfig> configRef,
            MotdTracker motdTracker,
            OnlineRoster roster,
            Supplier<PanelHttp> panelHttp,
            AeroIo aeroIo,
            Logger logger
    ) {
        this.configRef = configRef;
        this.motdTracker = motdTracker;
        this.roster = roster;
        this.panelHttp = panelHttp;
        this.aeroIo = aeroIo;
        this.logger = logger;
    }

    public void start() {
        stop();
        if (aeroIo == null) {
            return;
        }
        task = aeroIo.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                schedulePush();
            }
        }, PERIOD_MS, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    private void schedulePush() {
        AeroConfig config = configRef.get();
        if (config == null || !config.isPanelConfigured() || pushInFlight.get()) {
            return;
        }
        final String motd = motdTracker.motd();
        final int players = roster.size();
        final int max = roster.maxPlayers();
        if (unchanged(motd, players, max) || !pushInFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            pushStatus(config, motd, players, max);
        } finally {
            pushInFlight.set(false);
        }
    }

    private boolean unchanged(String motd, int players, int max) {
        return motd.equals(lastMotdSent) && players == lastPlayersSent && max == lastMaxSent;
    }

    private void pushStatus(AeroConfig cfg, String motd, int players, int max) {
        try {
            PanelHttp http = panelHttp == null ? null : panelHttp.get();
            PanelClient client = http == null
                    ? new HttpPanelClient(cfg)
                    : http.panelClient(cfg);
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
        }
    }
}
