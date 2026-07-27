package com.aelion.aero.common.config;

import com.aelion.aero.common.json.AeroJson;
import com.aelion.aero.common.util.Strings;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Cloud-owned (or manually created) identity file {@code aero.ae} — panel URL, tokens, control.
 * Operator knobs live in {@code config.yml}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class AeroIdentity {

    public static final String FILE_NAME = "aero.ae";

    @JsonProperty("panelUrl")
    private String panelUrl = "";

    @JsonProperty("serverId")
    private String serverId = "";

    @JsonProperty("token")
    private String token = "";

    @JsonProperty("panelInsecureSsl")
    private boolean panelInsecureSsl;

    @JsonProperty("control")
    private ControlIdentity control = new ControlIdentity();

    public AeroIdentity() {
    }

    public static Path pathIn(Path dataDirectory) {
        return dataDirectory.resolve(FILE_NAME);
    }

    public static AeroIdentity empty() {
        return new AeroIdentity();
    }

    public static AeroIdentity load(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            return empty();
        }
        try (InputStream in = Files.newInputStream(path)) {
            AeroIdentity identity = AeroJson.mapper().readValue(in, AeroIdentity.class);
            return identity == null ? empty() : identity;
        }
    }

    public static void write(Path path, AeroIdentity identity) throws IOException {
        Files.createDirectories(path.getParent());
        byte[] bytes = AeroJson.mapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(identity);
        Files.write(path, bytes);
    }

    /**
     * Build identity from legacy {@code config.yml} keys (pre two-file layout).
     */
    public static AeroIdentity fromLegacyConfigMap(java.util.Map<String, Object> root) {
        AeroIdentity identity = new AeroIdentity();
        if (root == null) {
            return identity;
        }
        identity.panelUrl = stringVal(root.get("panel-url"));
        identity.serverId = stringVal(root.get("server-id"));
        identity.token = stringVal(root.get("token"));
        identity.panelInsecureSsl = boolVal(root.get("panel-insecure-ssl"), false);
        Object controlObj = root.get("control");
        if (controlObj instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) controlObj;
            identity.control = ControlIdentity.fromMap(map);
        }
        return identity;
    }

    public boolean hasAnyIdentityFields() {
        return Strings.isNotBlank(panelUrl)
                || Strings.isNotBlank(serverId)
                || Strings.isNotBlank(token)
                || (control != null && control.hasAnyFields());
    }

    /**
     * Overlay identity onto a base config (typically operator YAML + defaults).
     * Identity fields always win when this file is present.
     */
    public AeroConfig applyOnto(AeroConfig base) {
        AeroConfig.ControlConfig baseControl =
                base == null ? AeroConfig.ControlConfig.disabled() : base.control();
        AeroConfig.ControlConfig mergedControl = control == null
                ? baseControl
                : control.toControlConfig(baseControl);
        String url = panelUrl == null ? "" : panelUrl.trim();
        String id = serverId == null ? "" : serverId.trim();
        String tok = token == null ? "" : token.trim();
        return new AeroConfig(url, id, tok, panelInsecureSsl, mergedControl);
    }

    public String panelUrl() {
        return panelUrl == null ? "" : panelUrl.trim();
    }

    public void setPanelUrl(String panelUrl) {
        this.panelUrl = panelUrl;
    }

    public String serverId() {
        return serverId == null ? "" : serverId.trim();
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String token() {
        return token == null ? "" : token.trim();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean panelInsecureSsl() {
        return panelInsecureSsl;
    }

    public void setPanelInsecureSsl(boolean panelInsecureSsl) {
        this.panelInsecureSsl = panelInsecureSsl;
    }

    public ControlIdentity control() {
        return control == null ? new ControlIdentity() : control;
    }

    public void setControl(ControlIdentity control) {
        this.control = control;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : Objects.toString(value, "").trim();
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class ControlIdentity {
        @JsonProperty("enabled")
        private boolean enabled;

        @JsonProperty("bind")
        private String bind = "127.0.0.1";

        @JsonProperty("port")
        private int port = 25580;

        @JsonProperty("token")
        private String token = "";

        public ControlIdentity() {
        }

        static ControlIdentity fromMap(java.util.Map<String, Object> map) {
            ControlIdentity c = new ControlIdentity();
            c.enabled = boolVal(map.get("enabled"), false);
            String bind = stringVal(map.get("bind"));
            c.bind = bind.isEmpty() ? "127.0.0.1" : bind;
            c.port = intVal(map.get("port"), 25580);
            c.token = stringVal(map.get("token"));
            return c;
        }

        boolean hasAnyFields() {
            return enabled || Strings.isNotBlank(token) || (port > 0 && port != 25580)
                    || (bind != null && !"127.0.0.1".equals(bind.trim()));
        }

        AeroConfig.ControlConfig toControlConfig(AeroConfig.ControlConfig fallback) {
            // Incomplete aero.ae (panel identity only, no control block) must not wipe
            // operator YAML control with Java defaults (enabled=false, empty token).
            if (!hasAnyFields()) {
                return fallback;
            }
            String b = Strings.isBlank(bind) ? fallback.bind() : bind.trim();
            int p = port <= 0 ? fallback.port() : port;
            String t = Strings.isBlank(token) ? fallback.token() : token.trim();
            return new AeroConfig.ControlConfig(enabled, b, p, t);
        }

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String bind() {
            return bind;
        }

        public void setBind(String bind) {
            this.bind = bind;
        }

        public int port() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String token() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
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
