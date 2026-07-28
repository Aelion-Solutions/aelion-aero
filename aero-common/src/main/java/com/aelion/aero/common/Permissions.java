package com.aelion.aero.common;

import java.util.function.Predicate;

/**
 * Bukkit / Velocity permission nodes and shared allow checks.
 *
 * <p>{@code admin} and {@code create} each imply {@code info} in code (Velocity has no YAML
 * children). They do not imply each other.
 */
public final class Permissions {

    public static final String INFO = "aelion.aero.info";
    public static final String ADMIN = "aelion.aero.admin";
    /** Create servers/groups via panel (`/ae create-*`, proxy only). */
    public static final String CREATE = "aelion.aero.create";

    private Permissions() {
    }

    /** Read / help / ping / servers / backends / notify — INFO, or ADMIN/CREATE (imply info). */
    public static boolean allowsInfo(Predicate<String> hasPermission) {
        return hasPermission.test(INFO)
                || hasPermission.test(ADMIN)
                || hasPermission.test(CREATE);
    }

    /** Reload / kick / transfer. */
    public static boolean allowsAdmin(Predicate<String> hasPermission) {
        return hasPermission.test(ADMIN);
    }

    /** Proxy create-server (does not imply admin). */
    public static boolean allowsCreate(Predicate<String> hasPermission) {
        return hasPermission.test(CREATE);
    }

    /** Outer command gate: any Aero node. */
    public static boolean allowsAny(Predicate<String> hasPermission) {
        return hasPermission.test(INFO)
                || hasPermission.test(ADMIN)
                || hasPermission.test(CREATE);
    }
}
