package com.aelion.aero.common;

/**
 * Shared constants for Aelion Aero plugins.
 */
public final class AeroConstants {

    public static final String NAME = "AelionAero";

    /** Primary root on Velocity/Bungee. */
    public static final String COMMAND_PROXY_PRIMARY = "ae";
    /** Alias for {@link #COMMAND_PROXY_PRIMARY}. */
    public static final String COMMAND_PROXY_ALIAS = "aec";
    /** Root on Bukkit/Paper backend bands. */
    public static final String COMMAND_BACKEND_PRIMARY = "aes";

    private AeroConstants() {
    }

    /** Slash-command root for help/usage strings. */
    public static String commandRoot(boolean proxy) {
        return proxy ? COMMAND_PROXY_PRIMARY : COMMAND_BACKEND_PRIMARY;
    }
}
