package com.aelion.aero.common.config;

import com.aelion.aero.common.ControlApi;
import com.aelion.aero.common.util.Strings;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Runtime configuration shared by Paper and Velocity.
 */
public final class AeroConfig {

    private final String panelUrl;
    private final String serverId;
    private final String token;
    private final ControlConfig control;

    public AeroConfig(String panelUrl, String serverId, String token, ControlConfig control) {
        this.panelUrl = panelUrl == null ? "" : panelUrl.trim();
        this.serverId = serverId == null ? "" : serverId.trim();
        this.token = token == null ? "" : token.trim();
        this.control = control == null ? ControlConfig.disabled() : control;
    }

    public String panelUrl() {
        return panelUrl;
    }

    public String serverId() {
        return serverId;
    }

    public String token() {
        return token;
    }

    public ControlConfig control() {
        return control;
    }

    public boolean isPanelConfigured() {
        return !panelUrl.isEmpty() && !token.isEmpty() && !serverId.isEmpty();
    }

    /**
     * Host portion of panel URL for display (never includes credentials).
     */
    public String panelHostForDisplay() {
        if (panelUrl.isEmpty()) {
            return "(not set)";
        }
        try {
            java.net.URI uri = java.net.URI.create(panelUrl);
            String host = uri.getHost();
            return Strings.isBlank(host) ? panelUrl : host;
        } catch (IllegalArgumentException e) {
            return "(invalid url)";
        }
    }

    @SuppressWarnings("unchecked")
    public static AeroConfig fromMap(Map<String, Object> root) {
        if (root == null) {
            return new AeroConfig("", "", "", ControlConfig.disabled());
        }
        String panelUrl = stringVal(root.get("panel-url"));
        String serverId = stringVal(root.get("server-id"));
        String token = stringVal(root.get("token"));
        Object controlObj = root.get("control");
        ControlConfig control = ControlConfig.disabled();
        if (controlObj instanceof Map) {
            control = ControlConfig.fromMap((Map<String, Object>) controlObj);
        }
        return new AeroConfig(panelUrl, serverId, token, control);
    }

    private static String stringVal(Object value) {
        return value == null ? "" : Objects.toString(value, "").trim();
    }

    public static final class ControlConfig {
        private final boolean enabled;
        private final String bind;
        private final int port;
        private final String token;

        public ControlConfig(boolean enabled, String bind, int port, String token) {
            this.enabled = enabled;
            this.bind = Strings.isBlank(bind) ? ControlApi.DEFAULT_BIND : bind.trim();
            this.port = port <= 0 ? ControlApi.DEFAULT_PORT : port;
            this.token = token == null ? "" : token.trim();
        }

        public static ControlConfig disabled() {
            return new ControlConfig(false, ControlApi.DEFAULT_BIND, ControlApi.DEFAULT_PORT, "");
        }

        public boolean enabled() {
            return enabled;
        }

        public String bind() {
            return bind;
        }

        public int port() {
            return port;
        }

        public String token() {
            return token;
        }

        public boolean isLoopbackBind() {
            String host = bind.toLowerCase(Locale.ROOT);
            return "127.0.0.1".equals(host)
                    || "localhost".equals(host)
                    || "::1".equals(host)
                    || "[::1]".equals(host);
        }

        public static ControlConfig fromMap(Map<String, Object> map) {
            boolean enabled = boolVal(map.get("enabled"), false);
            String bind = stringVal(map.get("bind"));
            if (bind.isEmpty()) {
                bind = ControlApi.DEFAULT_BIND;
            }
            int port = intVal(map.get("port"), ControlApi.DEFAULT_PORT);
            String token = stringVal(map.get("token"));
            return new ControlConfig(enabled, bind, port, token);
        }

        private static boolean boolVal(Object value, boolean defaultValue) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            if (value == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(Objects.toString(value));
        }

        private static int intVal(Object value, int defaultValue) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(Objects.toString(value).trim());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }
}
