package com.aelion.aero.common.api;

/**
 * Thrown when panelUrl / serverId / token are incomplete (aero.ae / legacy config).
 */
public final class PanelNotConfiguredException extends RuntimeException {

    public PanelNotConfiguredException() {
        super("Aelion Cloud panel is not configured (aero.ae: panelUrl, serverId, token)");
    }
}
