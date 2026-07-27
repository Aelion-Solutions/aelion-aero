package com.aelion.aero.paper.fleet;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.api.FleetGroupSnapshot;
import com.aelion.aero.api.FleetServerSnapshot;
import com.aelion.aero.common.api.GroupInfoResponse;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.config.AeroConfig;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Cached panel fleet access + BungeeCord Connect for first-party plugins.
 */
public final class PaperFleetService implements AeroFleetService, PluginMessageListener {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final long DEFAULT_TTL_MS = 2_000L;

    private final JavaPlugin plugin;
    private final Logger logger;
    private final AtomicReference<AeroConfig> configRef;
    private final long ttlMs;

    private volatile List<FleetServerSnapshot> servers = List.of();
    private volatile List<FleetGroupSnapshot> groups = List.of();
    private volatile long fetchedAtMs;
    private volatile String lastError;

    public PaperFleetService(JavaPlugin plugin, AtomicReference<AeroConfig> configRef) {
        this(plugin, configRef, DEFAULT_TTL_MS);
    }

    public PaperFleetService(JavaPlugin plugin, AtomicReference<AeroConfig> configRef, long ttlMs) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configRef = configRef;
        this.ttlMs = Math.max(500L, ttlMs);
    }

    public void start() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public void stop() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public String lastError() {
        return lastError;
    }

    @Override
    public boolean isConfigured() {
        AeroConfig config = configRef.get();
        return config != null && config.isPanelConfigured();
    }

    @Override
    public synchronized void refresh() {
        AeroConfig config = configRef.get();
        if (config == null || !config.isPanelConfigured()) {
            lastError = "Aero panel not configured";
            return;
        }
        try {
            HttpPanelClient client = new HttpPanelClient(config);
            List<ServerInfoResponse> serverResponses = client.listServers();
            List<GroupInfoResponse> groupResponses = client.listGroups();
            this.servers = mapServers(serverResponses);
            this.groups = mapGroups(groupResponses);
            this.fetchedAtMs = System.currentTimeMillis();
            this.lastError = null;
        } catch (PanelNotConfiguredException e) {
            lastError = "Aero panel not configured";
        } catch (PanelApiException e) {
            lastError = "Panel HTTP " + e.statusCode() + ": " + e.getMessage();
            logger.log(Level.WARNING, "Fleet refresh failed: " + lastError);
        } catch (RuntimeException e) {
            lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            logger.log(Level.WARNING, "Fleet refresh failed", e);
        }
    }

    @Override
    public List<FleetServerSnapshot> listServers() {
        ensureFresh();
        return servers;
    }

    @Override
    public List<FleetGroupSnapshot> listGroups() {
        ensureFresh();
        return groups;
    }

    @Override
    public boolean connectPlayer(UUID playerId, String proxyServerName) {
        if (playerId == null || proxyServerName == null || proxyServerName.isBlank()) {
            return false;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return false;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF("Connect");
            out.writeUTF(proxyServerName.trim());
            player.sendPluginMessage(plugin, BUNGEE_CHANNEL, bytes.toByteArray());
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send Connect for " + player.getName(), e);
            return false;
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Outgoing Connect only; ignore replies.
    }

    private void ensureFresh() {
        if (System.currentTimeMillis() - fetchedAtMs > ttlMs) {
            refresh();
        }
    }

    private static List<FleetServerSnapshot> mapServers(List<ServerInfoResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }
        List<FleetServerSnapshot> out = new ArrayList<>(responses.size());
        for (ServerInfoResponse r : responses) {
            out.add(mapServer(r));
        }
        return Collections.unmodifiableList(out);
    }

    private static FleetServerSnapshot mapServer(ServerInfoResponse r) {
        String proxy = r.getProxyName() == null || r.getProxyName().isBlank() ? r.getName() : r.getProxyName();
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
                proxy
        );
    }

    private static List<FleetGroupSnapshot> mapGroups(List<GroupInfoResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }
        List<FleetGroupSnapshot> out = new ArrayList<>(responses.size());
        for (GroupInfoResponse g : responses) {
            List<FleetServerSnapshot> members = new ArrayList<>();
            if (g.getMembers() != null) {
                for (GroupInfoResponse.GroupMemberInfo m : g.getMembers()) {
                    String proxy = m.getProxyName() == null || m.getProxyName().isBlank()
                            ? m.getName()
                            : m.getProxyName();
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
                            proxy
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
