package com.aelion.aero.bukkit;

import com.aelion.aero.common.ControlApi;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.ControlHealthResponse;
import com.aelion.aero.common.control.ControlKickRequest;
import com.aelion.aero.common.control.ControlPlayerActionResponse;
import com.aelion.aero.common.control.ControlShutdownResponse;
import com.aelion.aero.common.control.ControlTransferRequest;
import com.aelion.aero.common.control.ControlTransferResolver;
import com.aelion.aero.common.json.AeroJson;
import com.aelion.aero.common.util.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loopback-only HTTP control plane for the daemon (backends).
 * Health, graceful shutdown, player kick/transfer. No proxy backends registry.
 */
final class BukkitControlHttpServer {

    private static final String SHUTDOWN_KICK = ChatColor.YELLOW + "Server is shutting down.";
    private static final String DEFAULT_KICK = ChatColor.YELLOW + "Kicked by Aelion Aero.";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final AtomicReference<AeroConfig> configRef;
    private final Supplier<BukkitFleetService> fleetSupplier;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private HttpServer server;

    BukkitControlHttpServer(
            JavaPlugin plugin,
            AtomicReference<AeroConfig> configRef,
            Supplier<BukkitFleetService> fleetSupplier
    ) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configRef = configRef;
        this.fleetSupplier = fleetSupplier;
    }

    synchronized void start(AeroConfig.ControlConfig control) throws IOException {
        stop();
        shutdownRequested.set(false);
        if (!control.enabled()) {
            return;
        }
        if (!control.isLoopbackBind()) {
            throw new IOException("control.bind must be loopback (127.0.0.1 / ::1), got: " + control.bind());
        }
        if (control.token().isEmpty()) {
            throw new IOException("control.token is required when control.enabled=true");
        }

        InetSocketAddress address = new InetSocketAddress(control.bind(), control.port());
        server = HttpServer.create(address, 0);
        String expectedToken = control.token();

        server.createContext(ControlApi.HEALTH_PATH, exchange -> {
            if (!authorize(exchange, expectedToken)) {
                return;
            }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            sendJson(exchange, 200, ControlHealthResponse.ok());
        });

        server.createContext(ControlApi.SHUTDOWN_PATH, exchange -> {
            if (!authorize(exchange, expectedToken)) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            sendJson(exchange, 202, ControlShutdownResponse.accepted());
            scheduleGracefulShutdown();
        });

        server.createContext(ControlApi.PLAYERS_KICK_PATH, exchange -> {
            if (!authorize(exchange, expectedToken)) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            handleKick(exchange);
        });

        server.createContext(ControlApi.PLAYERS_TRANSFER_PATH, exchange -> {
            if (!authorize(exchange, expectedToken)) {
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "{\"error\":\"method not allowed\"}");
                return;
            }
            handleTransfer(exchange);
        });

        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "aero-control");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        logger.info("Aero control API listening on " + control.bind() + ":" + control.port());
    }

    synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handleKick(HttpExchange exchange) throws IOException {
        ControlKickRequest req;
        try (InputStream in = exchange.getRequestBody()) {
            req = AeroJson.mapper().readValue(in, ControlKickRequest.class);
        } catch (Exception e) {
            send(exchange, 400, "{\"error\":\"invalid kick payload\"}");
            return;
        }
        UUID uuid = parseUuid(req == null ? null : req.getUuid());
        if (uuid == null) {
            send(exchange, 400, "{\"error\":\"uuid is required\"}");
            return;
        }
        final String message = req.getMessage();
        final boolean[] ok = {false};
        try {
            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.kickPlayer(Strings.isBlank(message) ? DEFAULT_KICK : message);
                    ok[0] = true;
                }
                return Boolean.TRUE;
            }).get();
        } catch (Exception e) {
            send(exchange, 500, "{\"error\":\"kick failed\"}");
            return;
        }
        if (!ok[0]) {
            send(exchange, 404, "{\"error\":\"player offline\"}");
            return;
        }
        sendJson(exchange, 200, ControlPlayerActionResponse.ok());
    }

    private void handleTransfer(HttpExchange exchange) throws IOException {
        ControlTransferRequest req;
        try (InputStream in = exchange.getRequestBody()) {
            req = AeroJson.mapper().readValue(in, ControlTransferRequest.class);
        } catch (Exception e) {
            send(exchange, 400, "{\"error\":\"invalid transfer payload\"}");
            return;
        }
        UUID uuid = parseUuid(req == null ? null : req.getUuid());
        if (uuid == null) {
            send(exchange, 400, "{\"error\":\"uuid is required\"}");
            return;
        }
        BukkitFleetService fleet = fleetSupplier == null ? null : fleetSupplier.get();
        ControlTransferResolver.Result resolved = ControlTransferResolver.resolve(
                req,
                configRef.get(),
                fleet == null ? Collections.emptyList() : fleet.listServers(),
                fleet == null ? Collections.emptyList() : fleet.listGroups(),
                null);
        if (!resolved.isOk()) {
            send(exchange, 400, "{\"error\":" + AeroJson.mapper().writeValueAsString(resolved.error()) + "}");
            return;
        }
        final boolean[] ok = {false};
        try {
            Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                if (fleet != null) {
                    ok[0] = fleet.connectPlayer(uuid, resolved.proxyServerName());
                }
                return Boolean.TRUE;
            }).get();
        } catch (Exception e) {
            send(exchange, 500, "{\"error\":\"transfer failed\"}");
            return;
        }
        if (!ok[0]) {
            send(exchange, 404, "{\"error\":\"player offline or connect failed\"}");
            return;
        }
        sendJson(exchange, 200, ControlPlayerActionResponse.ok());
    }

    private void scheduleGracefulShutdown() {
        if (!shutdownRequested.compareAndSet(false, true)) {
            return;
        }
        logger.info("Control API requested graceful shutdown");
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(SHUTDOWN_KICK);
            }
            Bukkit.getServer().shutdown();
        });
    }

    private static UUID parseUuid(String raw) {
        if (Strings.isBlank(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean authorize(HttpExchange exchange, String expectedToken) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        String provided = headers.getFirst(ControlApi.TOKEN_HEADER);
        if (provided == null || !provided.equals(expectedToken)) {
            send(exchange, 401, "{\"error\":\"unauthorized\"}");
            return false;
        }
        return true;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, AeroJson.mapper().writeValueAsString(body));
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
