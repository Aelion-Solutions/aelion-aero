package com.aelion.aero.common;

/**
 * Plugin version reported to operators and control health checks.
 * Keep in sync with {@code gradle.properties} until build-time injection exists.
 */
public final class AeroVersion {

    public static final String VERSION = "0.2.0"; // x-release-please-version

    private AeroVersion() {
    }
}
