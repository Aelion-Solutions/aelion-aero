package com.aelion.aero.common.api;

import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.json.AeroJson;
import com.aelion.aero.common.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * {@link PanelClient} using OkHttp and Bearer server token.
 */
public final class HttpPanelClient implements PanelClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final TypeReference<List<ServerInfoResponse>> SERVER_LIST_TYPE =
            new TypeReference<List<ServerInfoResponse>>() {
            };
    private static final TypeReference<List<GroupInfoResponse>> GROUP_LIST_TYPE =
            new TypeReference<List<GroupInfoResponse>>() {
            };

    private final AeroConfig config;
    private final OkHttpClient httpClient;

    public HttpPanelClient(AeroConfig config) {
        this(config, defaultClient());
    }

    public HttpPanelClient(AeroConfig config, OkHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient == null ? defaultClient() : httpClient;
    }

    private static OkHttpClient defaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .build();
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
    public List<ServerInfoResponse> listServers() {
        requireConfigured();
        Request request = baseRequest(PanelPaths.SERVERS).get().build();
        return sendList(request, SERVER_LIST_TYPE);
    }

    @Override
    public List<GroupInfoResponse> listGroups() {
        requireConfigured();
        Request request = baseRequest(PanelPaths.GROUPS).get().build();
        return sendList(request, GROUP_LIST_TYPE);
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
        Request request = baseRequest(path).get().build();
        return send(request, type);
    }

    private <T> T post(String path, Object body, Class<T> type) {
        String json;
        try {
            json = AeroJson.mapper().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new PanelApiException(0, "Failed to encode request: " + e.getMessage());
        }
        Request request = baseRequest(path)
                .post(RequestBody.create(json, JSON))
                .build();
        return send(request, type);
    }

    private Request.Builder baseRequest(String path) {
        String base = trimTrailingSlash(config.panelUrl());
        return new Request.Builder()
                .url(base + path)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + config.token())
                .header("User-Agent", "AelionAero/" + AeroVersion.VERSION);
    }

    private <T> T send(Request request, Class<T> type) {
        try (Response response = httpClient.newCall(request).execute()) {
            int status = response.code();
            String body = readBody(response);
            if (status < 200 || status >= 300) {
                throw new PanelApiException(status, body);
            }
            if (Strings.isBlank(body)) {
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new PanelApiException(status, "Empty response body");
                }
            }
            return AeroJson.mapper().readValue(body, type);
        } catch (PanelApiException e) {
            throw e;
        } catch (IOException e) {
            throw new PanelApiException(0, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private <T> List<T> sendList(Request request, TypeReference<List<T>> type) {
        try (Response response = httpClient.newCall(request).execute()) {
            int status = response.code();
            String body = readBody(response);
            if (status < 200 || status >= 300) {
                throw new PanelApiException(status, body);
            }
            if (Strings.isBlank(body)) {
                return Collections.emptyList();
            }
            List<T> list = AeroJson.mapper().readValue(body, type);
            return list == null ? Collections.emptyList() : list;
        } catch (PanelApiException e) {
            throw e;
        } catch (IOException e) {
            throw new PanelApiException(0, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }

    private static String readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
