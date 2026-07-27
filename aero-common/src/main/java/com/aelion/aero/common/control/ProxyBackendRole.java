package com.aelion.aero.common.control;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.aelion.aero.common.util.Strings;
import java.util.Locale;

/**
 * Proxy relation role (REST wire strings).
 */
public enum ProxyBackendRole {
    BACKEND("backend"),
    LOBBY("lobby"),
    TRY("try");

    private final String wire;

    ProxyBackendRole(String wire) {
        this.wire = wire;
    }

    @JsonValue
    public String wire() {
        return wire;
    }

    @JsonCreator
    public static ProxyBackendRole fromWire(String value) {
        if (Strings.isBlank(value)) {
            return BACKEND;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ProxyBackendRole role : values()) {
            if (role.wire.equals(normalized)) {
                return role;
            }
        }
        return BACKEND;
    }

    public boolean isTryListRole() {
        return this == LOBBY || this == TRY;
    }
}
