package com.aelion.aero.common.api;

/**
 * Thrown when panel-url / server-id / token are incomplete.
 */
public final class PanelNotConfiguredException extends RuntimeException {

    public PanelNotConfiguredException() {
        super("Aelion Cloud panel is not configured (panel-url, server-id, token)");
    }
}
