package com.aelion.aero.common.api;

import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.json.AeroJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * {@link PanelClient} using {@link HttpClient} and Bearer server token.
 */
public final class HttpPanelClient implements PanelClient {

    private final AeroConfig config;
    private final HttpClient httpClient;
    private final Duration timeout;

    public HttpPanelClient(AeroConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), Duration.ofSeconds(15));
    }

    public HttpPanelClient(AeroConfig config, HttpClient httpClient, Duration timeout) {
        this.config = config;
        this.httpClient = httpClient;
        this.timeout = timeout == null ? Duration.ofSeconds(15) : timeout;
    }

    @Override
    public PanelHealthResponse ping() {
        requireConfigured();
        return get(PanelPaths.HEALTH, PanelHealthResponse.class);
    }

    @Override
    public ServerInfoResponse getServerInfo() {
        requireConfigured();
        return get(PanelPaths.server(config.serverId()), ServerInfoResponse.class);
    }

    @Override
    public CreateServerResponse createServer(CreateServerRequest request) {
        requireConfigured();
        return post(PanelPaths.SERVERS, request, CreateServerResponse.class);
    }

    @Override
    public CreateGroupResponse createGroup(CreateGroupRequest request) {
        requireConfigured();
        return post(PanelPaths.GROUPS, request, CreateGroupResponse.class);
    }

    private void requireConfigured() {
        if (!config.isPanelConfigured()) {
            throw new PanelNotConfiguredException();
        }
    }

    private <T> T get(String path, Class<T> type) {
        HttpRequest request = baseRequest(path).GET().build();
        return send(request, type);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        String json;
        try {
            json = AeroJson.mapper().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new PanelApiException(0, "Failed to encode request: " + e.getMessage());
        }
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return send(request, type);
    }

    private HttpRequest.Builder baseRequest(String path) {
        String base = trimTrailingSlash(config.panelUrl());
        URI uri = URI.create(base + path);
        return HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + config.token())
                .header("User-Agent", "AelionAero/" + com.aelion.aero.common.AeroVersion.VERSION);
    }

    private <T> T send(HttpRequest request, Class<T> type) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String body = response.body() == null ? "" : response.body();
            if (status < 200 || status >= 300) {
                throw new PanelApiException(status, body);
            }
            if (body.isBlank()) {
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new PanelApiException(status, "Empty response body");
                }
            }
            return AeroJson.mapper().readValue(body, type);
        } catch (PanelApiException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new PanelApiException(0, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
