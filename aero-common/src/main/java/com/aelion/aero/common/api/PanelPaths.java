package com.aelion.aero.common.api;

/**
 * Intended Aero panel REST paths (not necessarily live in cloud yet).
 */
public final class PanelPaths {

    public static final String V1 = "/api/aero/v1";
    public static final String HEALTH = V1 + "/health";
    public static final String SERVERS = V1 + "/servers";
    public static final String GROUPS = V1 + "/groups";

    private PanelPaths() {
    }

    public static String server(String serverId) {
        return SERVERS + "/" + serverId;
    }
}
