package com.aelion.aero.common.command;

/**
 * Shared legacy (§) styling for operator chat across Paper, Velocity, and Bungee.
 */
public final class AeroCommandStyle {

    public static final String PREFIX = "§8[§bAero§8]§r ";

    private AeroCommandStyle() {
    }

    public static String info(String message) {
        return PREFIX + "§7" + message + "§r";
    }

    public static String success(String message) {
        return PREFIX + "§a" + message + "§r";
    }

    public static String warn(String message) {
        return PREFIX + "§e" + message + "§r";
    }

    public static String error(String message) {
        return PREFIX + "§c" + message + "§r";
    }

    public static String label(String key, String value) {
        return PREFIX + "§7" + key + ": §f" + value + "§r";
    }
}
