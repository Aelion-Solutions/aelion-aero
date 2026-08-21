package com.aelion.aero.bukkit;

import com.aelion.aero.api.AeroFleetService;
import com.aelion.aero.api.FleetGroupSnapshot;
import com.aelion.aero.api.FleetServerSnapshot;
import com.aelion.aero.common.AeroIo;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelClient;
import com.aelion.aero.common.api.PanelHttp;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.fleet.FleetSnapshotCache;
import com.aelion.aero.common.fleet.FleetTransferResolver;
import com.aelion.aero.common.util.Strings;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Cached panel fleet access + BungeeCord Connect for first-party plugins.
 *
 * <p>{@link #listServers()} / {@link #listGroups()} are volatile reads. Panel I/O runs on
 * {@link AeroIo}, never on the game thread.
 */
public final class BukkitFleetService implements AeroFleetService, PluginMessageListener {

    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String DEFAULT_KICK_MESSAGE = ChatColor.YELLOW + "Kicked by Aelion Aero.";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final AtomicReference<AeroConfig> configRef;
    private final FleetSnapshotCache cache;
    private final AeroIo aeroIo;

    public BukkitFleetService(
            JavaPlugin plugin,
            AtomicReference<AeroConfig> configRef,
            Supplier<PanelHttp> panelHttp,
            AeroIo aeroIo
    ) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configRef = configRef;
        this.aeroIo = aeroIo;
        Function<AeroConfig, PanelClient> clients = config -> {
            PanelHttp http = panelHttp == null ? null : panelHttp.get();
            return http == null
                    ? new HttpPanelClient(config)
                    : http.panelClient(config);
        };
        this.cache = new FleetSnapshotCache(
                configRef::get,
                clients,
                (message, thrown) -> {
                    if (thrown == null) {
                        logger.log(Level.WARNING, message);
                    } else {
                        logger.log(Level.WARNING, message, thrown);
                    }
                });
    }

    public void start() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
        cache.start(aeroIo);
    }

    public void stop() {
        cache.stop();
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, this);
    }

    public String lastError() {
        return cache.lastError();
    }

    @Override
    public boolean isConfigured() {
        AeroConfig config = configRef.get();
        return config != null && config.isPanelConfigured();
    }

    @Override
    public void refresh() {
        cache.refresh();
    }

    @Override
    public List<FleetServerSnapshot> listServers() {
        return cache.listServers();
    }

    @Override
    public List<FleetGroupSnapshot> listGroups() {
        return cache.listGroups();
    }

    @Override
    public boolean connectPlayer(UUID playerId, String proxyServerName) {
        if (playerId == null || Strings.isBlank(proxyServerName)) {
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
    public boolean kickPlayer(UUID playerId, String message) {
        if (playerId == null) {
            return false;
        }
        String reason = Strings.isBlank(message) ? DEFAULT_KICK_MESSAGE : message;
        if (Bukkit.isPrimaryThread()) {
            return kickNow(playerId, reason);
        }
        try {
            return Boolean.TRUE.equals(
                    Bukkit.getScheduler().callSyncMethod(plugin, () -> kickNow(playerId, reason)).get());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to kick player " + playerId, e);
            return false;
        }
    }

    private static boolean kickNow(UUID playerId, String reason) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return false;
        }
        player.kickPlayer(reason);
        return true;
    }

    @Override
    public boolean transferToServer(UUID playerId, String serverIdOrName) {
        FleetServerSnapshot target = FleetTransferResolver.findServer(listServers(), serverIdOrName);
        String proxy = FleetTransferResolver.proxyNameOf(target);
        return connectPlayer(playerId, proxy);
    }

    @Override
    public boolean transferToGroup(UUID playerId, String groupIdOrName) {
        FleetGroupSnapshot group = FleetTransferResolver.findGroup(listGroups(), groupIdOrName);
        FleetServerSnapshot member = FleetTransferResolver.pickJoinableMember(group);
        String proxy = FleetTransferResolver.proxyNameOf(member);
        return connectPlayer(playerId, proxy);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Outgoing Connect only; ignore replies.
    }
}
