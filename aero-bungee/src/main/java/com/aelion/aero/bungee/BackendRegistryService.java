package com.aelion.aero.bungee;

import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.BackendRegistry;
import com.aelion.aero.common.control.ConnectionTactics;
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
import java.util.logging.Logger;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Applies full-replace backend registries to the live Bungee server map.
 */
final class BackendRegistryService {

    private static final String NO_LOBBY = "No lobby available. Disconnected.";
    private static final String SERVER_OFFLINE = "Your server went offline. Sending you to a lobby…";

    private final ProxyServer proxy;
    private final Logger logger;
    private final AtomicReference<BackendRegistry> lastApplied =
            new AtomicReference<>(new BackendRegistry());
    private final ConnectionTactics.RoundRobinState roundRobin = new ConnectionTactics.RoundRobinState();

    BackendRegistryService(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    BackendRegistry snapshot() {
        return lastApplied.get();
    }

    Optional<ServerInfo> resolveInitialServer(String excludeName) {
        BackendRegistry registry = lastApplied.get();
        List<ConnectionTactics.Candidate> candidates = ConnectionTactics.candidatesFrom(
                registry.validBackends(),
                name -> {
                    ServerInfo info = proxy.getServerInfo(sanitizeName(name));
                    return info == null ? -1 : info.getPlayers().size();
                }
        );
        List<ConnectionTactics.Candidate> registered = new ArrayList<>();
        for (ConnectionTactics.Candidate c : candidates) {
            String name = sanitizeName(c.name());
            if (name.isEmpty()) {
                continue;
            }
            if (proxy.getServerInfo(name) != null) {
                registered.add(c);
            }
        }
        String picked = ConnectionTactics.pickInitialServer(registered, excludeName, roundRobin);
        if (picked != null) {
            ServerInfo info = proxy.getServerInfo(sanitizeName(picked));
            if (info != null) {
                return Optional.of(info);
            }
        }
        String exclude = excludeName == null ? "" : sanitizeName(excludeName);
        for (ServerInfo info : proxy.getServers().values()) {
            String name = sanitizeName(info.getName());
            if (!name.isEmpty() && !name.equals(exclude)) {
                return Optional.of(info);
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
        Map<String, ServerInfo> servers = proxy.getServers();

        for (Map.Entry<String, BackendEntry> desiredEntry : desiredByName.entrySet()) {
            String name = desiredEntry.getKey();
            BackendEntry entry = desiredEntry.getValue();
            InetSocketAddress address = parseAddress(entry.getAddress());
            if (address == null) {
                logger.warning("Skipping backend " + name + " — invalid address " + entry.getAddress());
                continue;
            }

            ServerInfo existing = servers.get(name);
            ServerInfo neu = proxy.constructServerInfo(name, address, "&1" + name, false);
            if (existing != null) {
                if (existing.getAddress().equals(address)) {
                    continue;
                }
                List<ProxiedPlayer> movers = new ArrayList<>(existing.getPlayers());
                servers.remove(name);
                servers.put(name, neu);
                for (ProxiedPlayer player : movers) {
                    player.sendMessage(TextComponent.fromLegacy(SERVER_OFFLINE));
                    player.connect(neu);
                }
                updated++;
            } else {
                servers.put(name, neu);
                registered++;
            }
        }

        lastApplied.set(new BackendRegistry(new ArrayList<>(desiredByName.values())));

        List<String> toRemove = new ArrayList<>();
        for (String name : new HashSet<>(servers.keySet())) {
            if (!desiredByName.containsKey(sanitizeName(name))) {
                toRemove.add(name);
            }
        }
        for (String name : toRemove) {
            ServerInfo leaving = servers.get(name);
            if (leaving != null) {
                evacuatePlayers(leaving, sanitizeName(name));
            }
            servers.remove(name);
            removed++;
        }

        logger.info("Applied backend registry: +" + registered + " ~" + updated + " -" + removed
                + " (total " + desiredByName.size() + ")");
        return new ApplyResult(registered, updated, removed, desiredByName.size());
    }

    private void evacuatePlayers(ServerInfo leaving, String leavingName) {
        Set<ProxiedPlayer> players = new HashSet<>(leaving.getPlayers());
        if (players.isEmpty()) {
            return;
        }
        Optional<ServerInfo> target = resolveInitialServer(leavingName);
        if (target.isPresent()) {
            ServerInfo lobby = target.get();
            logger.info("Evacuating " + players.size() + " player(s) from " + leavingName
                    + " → " + lobby.getName());
            for (ProxiedPlayer player : players) {
                player.sendMessage(TextComponent.fromLegacy(SERVER_OFFLINE));
                player.connect(lobby);
            }
            return;
        }
        logger.warning("No lobby/try target while removing " + leavingName
                + "; disconnecting " + players.size() + " player(s)");
        for (ProxiedPlayer player : players) {
            player.disconnect(TextComponent.fromLegacy(NO_LOBBY));
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
