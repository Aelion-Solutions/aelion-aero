package com.aelion.aero.common;

/**
 * Localhost control API contract (daemon → Aero plugin).
 */
public final class ControlApi {

    public static final String HEALTH_PATH = "/v1/health";
    public static final String BACKENDS_PATH = "/v1/backends";
    public static final String SHUTDOWN_PATH = "/v1/shutdown";
    public static final String PLAYERS_KICK_PATH = "/v1/players/kick";
    public static final String PLAYERS_TRANSFER_PATH = "/v1/players/transfer";
    public static final String TOKEN_HEADER = "X-Aero-Control-Token";

    public static final String DEFAULT_BIND = "127.0.0.1";
    public static final int DEFAULT_PORT = 25580;

    private ControlApi() {
    }
}
