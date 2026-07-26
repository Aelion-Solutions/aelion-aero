package com.aelion.aero.common.command;

import com.aelion.aero.common.AeroConstants;
import com.aelion.aero.common.AeroVersion;
import com.aelion.aero.common.config.AeroConfig;

/**
 * Shared operator-facing strings for /ae commands.
 */
public final class AeroCommandMessages {

    private AeroCommandMessages() {
    }

    public static String help() {
        return """
                %s v%s
                /%s help — show this help
                /%s info — plugin and panel status
                /%s reload — reload config
                /%s ping — ping the Aelion Cloud panel
                """.formatted(
                AeroConstants.NAME,
                AeroVersion.VERSION,
                AeroConstants.COMMAND_PRIMARY,
                AeroConstants.COMMAND_PRIMARY,
                AeroConstants.COMMAND_PRIMARY,
                AeroConstants.COMMAND_PRIMARY
        ).trim();
    }

    public static String info(AeroConfig config) {
        return """
                %s v%s
                server-id: %s
                panel: %s
                panel configured: %s
                control enabled: %s (%s:%d)
                """.formatted(
                AeroConstants.NAME,
                AeroVersion.VERSION,
                config.serverId().isEmpty() ? "(not set)" : config.serverId(),
                config.panelHostForDisplay(),
                config.isPanelConfigured(),
                config.control().enabled(),
                config.control().bind(),
                config.control().port()
        ).trim();
    }
}
