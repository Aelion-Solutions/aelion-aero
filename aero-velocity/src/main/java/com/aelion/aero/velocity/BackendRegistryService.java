package com.aelion.aero.velocity;

import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.BackendRegistry;
import com.aelion.aero.common.control.ProxyBackendRole;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

/**
 * Applies full-replace backend registries to the live Velocity proxy and keeps
 * players on a joinable lobby/try target without requiring a proxy restart.
 */
final class BackendRegistryService {

    private static final Component NO_LOBBY_DISCONNECT = Component.text(
            "No lobby available. Disconnected.",
            NamedTextColor.RED
    );
    private static final Component SERVER_OFFLINE_MOVE = Component.text(
            "Your server went offline. Sending you to a lobby…",
            NamedTextColor.YELLOW
    );

    private final ProxyServer proxy;
    private final Logger logger;
    private final AtomicReference<BackendRegistry> lastApplied =
            new AtomicReference<>(new BackendRegistry());

    BackendRegistryService(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    BackendRegistry snapshot() {
        return lastApplied.get();
    }

    /**
     * Prefer lobby, then try, then any other registered backend — excluding {@code excludeName}.
     */
    Optional<RegisteredServer> resolveInitialServer(String excludeName) {
        String exclude = excludeName == null ? "" : sanitizeName(excludeName);
        BackendRegistry registry = lastApplied.get();
        Optional<RegisteredServer> lobby = firstRegisteredWithRole(registry, ProxyBackendRole.LOBBY, exclude);
        if (lobby.isPresent()) {
            return lobby;
        }
        Optional<RegisteredServer> tryRole = firstRegisteredWithRole(registry, ProxyBackendRole.TRY, exclude);
        if (tryRole.isPresent()) {
            return tryRole;
        }
        for (BackendEntry entry : registry.validBackends()) {
            String name = sanitizeName(entry.getName());
            if (name.isEmpty() || name.equals(exclude)) {
                continue;
            }
            Optional<RegisteredServer> server = proxy.getServer(name);
            if (server.isPresent()) {
                return server;
            }
        }
        for (RegisteredServer server : proxy.getAllServers()) {
            String name = sanitizeName(server.getServerInfo().getName());
            if (!name.isEmpty() && !name.equals(exclude)) {
                return Optional.of(server);
            }
        }
        return Optional.empty();
    }

    synchronized ApplyResult apply(BackendRegistry registry) {
        BackendRegistry desired = registry == null ? new BackendRegistry() : registry;
        Map<String, BackendEntry> desiredByName = new HashMap<>();
        for (BackendEntry entry : desired.validBackends()) {
            String name = sanitizeName(entry.getName());
            if (name.isEmpty()) {
                continue;
            }
            desiredByName.put(name, entry);
        }

        int registered = 0;
        int updated = 0;
        int removed = 0;

        // Register / update desired servers first so evacuation has a target.
        for (Map.Entry<String, BackendEntry> desiredEntry : desiredByName.entrySet()) {
            String name = desiredEntry.getKey();
            BackendEntry entry = desiredEntry.getValue();
            InetSocketAddress address = parseAddress(entry.getAddress());
            if (address == null) {
                logger.warn("Skipping backend {} — invalid address {}", name, entry.getAddress());
                continue;
            }

            Optional<RegisteredServer> existing = proxy.getServer(name);
            if (existing.isPresent()) {
                ServerInfo current = existing.get().getServerInfo();
                if (current.getAddress().equals(address)) {
                    continue;
                }
                List<Player> movers = new ArrayList<>(existing.get().getPlayersConnected());
                proxy.unregisterServer(current);
                RegisteredServer replaced = proxy.registerServer(new ServerInfo(name, address));
                for (Player player : movers) {
                    player.sendMessage(SERVER_OFFLINE_MOVE);
                    player.createConnectionRequest(replaced).connectWithIndication().whenComplete((ok, error) -> {
                        if (error != null || !Boolean.TRUE.equals(ok)) {
                            Optional<RegisteredServer> fallback = resolveInitialServer(name);
                            if (fallback.isPresent()) {
                                player.createConnectionRequest(fallback.get()).fireAndForget();
                            } else {
                                player.disconnect(NO_LOBBY_DISCONNECT);
                            }
                        }
                    });
                }
                updated++;
            } else {
                proxy.registerServer(new ServerInfo(name, address));
                registered++;
            }
        }

        lastApplied.set(new BackendRegistry(new ArrayList<>(desiredByName.values())));

        List<RegisteredServer> toRemove = new ArrayList<>();
        for (RegisteredServer server : proxy.getAllServers()) {
            String name = sanitizeName(server.getServerInfo().getName());
            if (!desiredByName.containsKey(name)) {
                toRemove.add(server);
            }
        }
        for (RegisteredServer server : toRemove) {
            String name = sanitizeName(server.getServerInfo().getName());
            evacuatePlayers(server, name);
            proxy.unregisterServer(server.getServerInfo());
            removed++;
        }

        logger.info("Applied backend registry: +{} ~{} -{} (total {})",
                registered, updated, removed, desiredByName.size());
        return new ApplyResult(registered, updated, removed, desiredByName.size());
    }

    private Optional<RegisteredServer> firstRegisteredWithRole(
            BackendRegistry registry,
            ProxyBackendRole role,
            String exclude
    ) {
        for (BackendEntry entry : registry.validBackends()) {
            if (entry.getRole() != role) {
                continue;
            }
            String name = sanitizeName(entry.getName());
            if (name.isEmpty() || name.equals(exclude)) {
                continue;
            }
            Optional<RegisteredServer> server = proxy.getServer(name);
            if (server.isPresent()) {
                return server;
            }
        }
        return Optional.empty();
    }

    private void evacuatePlayers(RegisteredServer leaving, String leavingName) {
        Set<Player> players = new HashSet<>(leaving.getPlayersConnected());
        if (players.isEmpty()) {
            return;
        }

        Optional<RegisteredServer> target = resolveInitialServer(leavingName);
        if (target.isPresent()) {
            RegisteredServer lobby = target.get();
            logger.info("Evacuating {} player(s) from {} → {}",
                    players.size(), leavingName, lobby.getServerInfo().getName());
            for (Player player : players) {
                player.sendMessage(SERVER_OFFLINE_MOVE);
                player.createConnectionRequest(lobby).connectWithIndication().whenComplete((ok, error) -> {
                    if (error != null || !Boolean.TRUE.equals(ok)) {
                        player.disconnect(NO_LOBBY_DISCONNECT);
                    }
                });
            }
            return;
        }

        logger.warn("No lobby/try target while removing {}; disconnecting {} player(s)",
                leavingName, players.size());
        for (Player player : players) {
            player.disconnect(NO_LOBBY_DISCONNECT);
        }
    }

    static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "");
    }

    private static InetSocketAddress parseAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String trimmed = address.trim();
        int colon = trimmed.lastIndexOf(':');
        if (colon <= 0 || colon == trimmed.length() - 1) {
            return null;
        }
        String host = trimmed.substring(0, colon);
        try {
            int port = Integer.parseInt(trimmed.substring(colon + 1));
            if (port <= 0 || port > 65535) {
                return null;
            }
            return new InetSocketAddress(host, port);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    record ApplyResult(int registered, int updated, int removed, int total) {
    }
}
