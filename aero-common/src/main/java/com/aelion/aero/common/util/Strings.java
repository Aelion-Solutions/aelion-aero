package com.aelion.aero.common.util;

/**
 * Java 8-safe string helpers (avoid {@code String#isBlank} from Java 11).
 */
public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
