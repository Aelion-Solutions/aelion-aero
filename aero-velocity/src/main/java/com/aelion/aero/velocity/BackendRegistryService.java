package com.aelion.aero.velocity;

import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.control.BackendRegistry;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * Applies full-replace backend registries to the live Velocity proxy.
 */
final class BackendRegistryService {

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

    synchronized ApplyResult apply(BackendRegistry registry) {
        BackendRegistry desired = registry == null ? new BackendRegistry() : registry;
        Set<String> desiredNames = new HashSet<>();
        int registered = 0;
        int updated = 0;
        int removed = 0;

        for (BackendEntry entry : desired.validBackends()) {
            String name = sanitizeName(entry.getName());
            if (name.isEmpty()) {
                continue;
            }
            desiredNames.add(name);
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
                proxy.unregisterServer(current);
                proxy.registerServer(new ServerInfo(name, address));
                updated++;
            } else {
                proxy.registerServer(new ServerInfo(name, address));
                registered++;
            }

            if (entry.getRole().isTryListRole()) {
                logger.info("Backend {} registered with role {} (try-list is best-effort; Velocity API limited)",
                        name, entry.getRole().wire());
            }
        }

        for (RegisteredServer server : proxy.getAllServers()) {
            String name = server.getServerInfo().getName();
            if (!desiredNames.contains(name)) {
                proxy.unregisterServer(server.getServerInfo());
                removed++;
            }
        }

        lastApplied.set(new BackendRegistry(desired.validBackends()));
        logger.info("Applied backend registry: +{} ~{} -{} (total {})",
                registered, updated, removed, desiredNames.size());
        return new ApplyResult(registered, updated, removed, desiredNames.size());
    }

    private static String sanitizeName(String name) {
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
