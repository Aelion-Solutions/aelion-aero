package com.aelion.aero.velocity;

import com.aelion.aero.common.ControlApi;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendRegistry;
import com.aelion.aero.common.control.ControlHealthResponse;
import com.aelion.aero.common.json.AeroJson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.slf4j.Logger;

/**
 * Loopback-only HTTP control plane for the daemon.
 */
final class ControlHttpServer {

    private final Logger logger;
    private final BackendRegistryService registryService;
    private HttpServer server;

    ControlHttpServer(Logger logger, BackendRegistryService registryService) {
        this.logger = logger;
        this.registryService = registryService;
    }

    synchronized void start(AeroConfig.ControlConfig control) throws IOException {
        stop();
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

        server.createContext(ControlApi.BACKENDS_PATH, exchange -> {
            if (!authorize(exchange, expectedToken)) {
                return;
            }
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                sendJson(exchange, 200, registryService.snapshot());
                return;
            }
            if ("PUT".equalsIgnoreCase(method)) {
                try (InputStream in = exchange.getRequestBody()) {
                    BackendRegistry body = AeroJson.mapper().readValue(in, BackendRegistry.class);
                    BackendRegistryService.ApplyResult result = registryService.apply(body);
                    send(exchange, 200, AeroJson.mapper().writeValueAsString(result));
                } catch (Exception e) {
                    logger.warn("Failed to apply backends: {}", e.getMessage());
                    send(exchange, 400, "{\"error\":\"invalid backends payload\"}");
                }
                return;
            }
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
        });

        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "aero-control");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        logger.info("Aero control API listening on {}:{}", control.bind(), control.port());
    }

    synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
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
