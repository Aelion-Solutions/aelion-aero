package com.aelion.aero.common.fleet;

import com.aelion.aero.api.FleetGroupSnapshot;
import com.aelion.aero.api.FleetServerSnapshot;
import com.aelion.aero.common.AeroIo;
import com.aelion.aero.common.api.GroupInfoResponse;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelClient;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Stale-while-revalidate fleet snapshots. {@link #listServers()} / {@link #listGroups()} never
 * perform I/O. {@link #refresh()} is blocking and single-flight.
 */
public final class FleetSnapshotCache {

    public static final long DEFAULT_PERIOD_MS = 2_000L;

    private final Supplier<AeroConfig> configSupplier;
    private final Function<AeroConfig, PanelClient> clientFactory;
    private final BiConsumer<String, Throwable> warn;
    private final long periodMs;

    private final AtomicBoolean refreshInFlight = new AtomicBoolean(false);
    private volatile List<FleetServerSnapshot> servers = Collections.emptyList();
    private volatile List<FleetGroupSnapshot> groups = Collections.emptyList();
    private volatile String lastError;
    private ScheduledFuture<?> periodic;

    public FleetSnapshotCache(
            Supplier<AeroConfig> configSupplier,
            Function<AeroConfig, PanelClient> clientFactory,
            BiConsumer<String, Throwable> warn
    ) {
        this(configSupplier, clientFactory, warn, DEFAULT_PERIOD_MS);
    }

    public FleetSnapshotCache(
            Supplier<AeroConfig> configSupplier,
            Function<AeroConfig, PanelClient> clientFactory,
            BiConsumer<String, Throwable> warn,
            long periodMs
    ) {
        this.configSupplier = configSupplier;
        this.clientFactory = clientFactory;
        this.warn = warn == null
                ? new BiConsumer<String, Throwable>() {
                    @Override
                    public void accept(String message, Throwable thrown) {
                    }
                }
                : warn;
        this.periodMs = Math.max(500L, periodMs);
    }

    /**
     * Last successful server snapshot. Never triggers HTTP.
     */
    public List<FleetServerSnapshot> listServers() {
        return servers;
    }

    /**
     * Last successful group snapshot. Never triggers HTTP.
     */
    public List<FleetGroupSnapshot> listGroups() {
        return groups;
    }

    public String lastError() {
        return lastError;
    }

    /**
     * Blocking panel round-trip. Concurrent callers are dropped (single-flight). Must not run
     * on the Bukkit primary thread.
     */
    public void refresh() {
        AeroConfig config = configSupplier.get();
        if (config == null || !config.isPanelConfigured()) {
            lastError = "Aero panel not configured";
            return;
        }
        if (!refreshInFlight.compareAndSet(false, true)) {
            return;
        }
        try {
            PanelClient client = clientFactory.apply(config);
            List<ServerInfoResponse> serverResponses = client.listServers();
            List<GroupInfoResponse> groupResponses = client.listGroups();
            this.servers = mapServers(serverResponses);
            this.groups = mapGroups(groupResponses);
            this.lastError = null;
        } catch (PanelNotConfiguredException e) {
            lastError = "Aero panel not configured";
        } catch (PanelApiException e) {
            lastError = "Panel HTTP " + e.statusCode() + ": " + e.getMessage();
            warn.accept("Fleet refresh failed: " + lastError, null);
        } catch (RuntimeException e) {
            lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            warn.accept("Fleet refresh failed", e);
        } finally {
            refreshInFlight.set(false);
        }
    }

    public void start(AeroIo io) {
        stop();
        if (io == null) {
            return;
        }
        periodic = io.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        }, 0L, periodMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (periodic != null) {
            periodic.cancel(false);
            periodic = null;
        }
    }

    public static List<FleetServerSnapshot> mapServers(List<ServerInfoResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }
        List<FleetServerSnapshot> out = new ArrayList<FleetServerSnapshot>(responses.size());
        for (ServerInfoResponse r : responses) {
            out.add(mapServer(r));
        }
        return Collections.unmodifiableList(out);
    }

    public static FleetServerSnapshot mapServer(ServerInfoResponse r) {
        String proxy = Strings.isBlank(r.getProxyName()) ? r.getName() : r.getProxyName();
        return new FleetServerSnapshot(
                r.getId(),
                r.getName(),
                r.getStatus(),
                r.getSoftware(),
                r.getLiveStatus(),
                r.getCurrentPlayers(),
                r.getMaxPlayers(),
                r.getGroupId(),
                r.getGroupName(),
                r.isJoinable(),
                proxy,
                r.getMotd()
        );
    }

    public static List<FleetGroupSnapshot> mapGroups(List<GroupInfoResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }
        List<FleetGroupSnapshot> out = new ArrayList<FleetGroupSnapshot>(responses.size());
        for (GroupInfoResponse g : responses) {
            List<FleetServerSnapshot> members = new ArrayList<FleetServerSnapshot>();
            if (g.getMembers() != null) {
                for (GroupInfoResponse.GroupMemberInfo m : g.getMembers()) {
                    String proxy = Strings.isBlank(m.getProxyName()) ? m.getName() : m.getProxyName();
                    members.add(new FleetServerSnapshot(
                            m.getId(),
                            m.getName(),
                            null,
                            null,
                            m.getLiveStatus(),
                            m.getCurrentPlayers(),
                            m.getMaxPlayers(),
                            g.getId(),
                            g.getName(),
                            m.isJoinable(),
                            proxy,
                            m.getMotd()
                    ));
                }
            }
            out.add(new FleetGroupSnapshot(
                    g.getId(),
                    g.getName(),
                    g.getStatus(),
                    g.getCurrentPlayers(),
                    g.getMaxPlayers(),
                    g.getMemberCount(),
                    g.getLiveStatus(),
                    members
            ));
        }
        return Collections.unmodifiableList(out);
    }
}
