package com.aelion.aero.bukkit;

import com.aelion.aero.common.ControlApi;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.ControlHealthResponse;
import com.aelion.aero.common.control.ControlShutdownResponse;
import com.aelion.aero.common.json.AeroJson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loopback-only HTTP control plane for the daemon (backends).
 * Exposes health + graceful shutdown; backends do not host the proxy backends registry.
 */
final class BukkitControlHttpServer {

    private static final String KICK_MESSAGE = ChatColor.YELLOW + "Server is shutting down.";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);
    private HttpServer server;

    BukkitControlHttpServer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
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

    private void scheduleGracefulShutdown() {
        if (!shutdownRequested.compareAndSet(false, true)) {
            return;
        }
        logger.info("Control API requested graceful shutdown");
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer(KICK_MESSAGE);
            }
            Bukkit.getServer().shutdown();
        });
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
