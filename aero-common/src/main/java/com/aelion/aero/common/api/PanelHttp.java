package com.aelion.aero.common.api;

import com.aelion.aero.common.config.AeroConfig;
import java.io.Closeable;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

/**
 * One OkHttp client per plugin lifetime: tiny dispatcher, small connection pool.
 *
 * <p>Call sites must reuse {@link #panelClient(AeroConfig)} instead of constructing a new
 * {@link OkHttpClient} per request.
 */
public final class PanelHttp implements Closeable {

    private static final int MAX_IDLE_CONNECTIONS = 5;
    private static final long KEEP_ALIVE_MINUTES = 5L;

    private final boolean insecureSsl;
    private final ExecutorService dispatcherExecutor;
    private final OkHttpClient client;

    public PanelHttp(boolean insecureSsl) {
        this.insecureSsl = insecureSsl;
        this.client = newClient(insecureSsl);
        this.dispatcherExecutor = client.dispatcher().executorService();
    }

    /**
     * Builds a tuned client. Prefer a {@link PanelHttp} instance so {@link #close()} can
     * shut the dispatcher down; this factory is the fallback for one-off clients.
     */
    public static OkHttpClient newClient(boolean insecureSsl) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "aero-http");
            t.setDaemon(true);
            return t;
        });
        Dispatcher dispatcher = new Dispatcher(executor);
        dispatcher.setMaxRequests(2);
        dispatcher.setMaxRequestsPerHost(2);
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES, TimeUnit.MINUTES))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS);
        if (insecureSsl) {
            applyInsecureSsl(builder);
        }
        return builder.build();
    }

    public boolean insecureSsl() {
        return insecureSsl;
    }

    public OkHttpClient client() {
        return client;
    }

    public HttpPanelClient panelClient(AeroConfig config) {
        return new HttpPanelClient(config, client);
    }

    @Override
    public void close() {
        client.dispatcher().cancelAll();
        client.connectionPool().evictAll();
        dispatcherExecutor.shutdownNow();
    }

    private static void applyInsecureSsl(OkHttpClient.Builder builder) {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            TrustManager[] trustManagers = new TrustManager[] {trustAll};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            HostnameVerifier allowAll = (hostname, session) -> true;
            builder.sslSocketFactory(socketFactory, trustAll);
            builder.hostnameVerifier(allowAll);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Failed to enable panel-insecure-ssl", e);
        }
    }
}
