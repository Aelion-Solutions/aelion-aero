package com.aelion.aero.bukkit;

import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.config.AeroIdentity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Maps Bukkit {@code config.yml} (+ optional {@code aero.ae}) into {@link AeroConfig}.
 */
public final class BukkitConfigBridge {

    private BukkitConfigBridge() {
    }

    public static AeroConfig fromBukkit(FileConfiguration config) {
        return AeroConfig.fromMap(toMap(config));
    }

    /**
     * Fallback when primary loader fails: YAML + identity file if present.
     */
    public static AeroConfig fromBukkitMerged(Path dataDirectory, FileConfiguration config) {
        AeroConfig fromYaml = fromBukkit(config);
        Path identityPath = AeroIdentity.pathIn(dataDirectory);
        if (!Files.isRegularFile(identityPath)) {
            return fromYaml;
        }
        try {
            return AeroIdentity.load(identityPath).applyOnto(fromYaml);
        } catch (Exception e) {
            return fromYaml;
        }
    }

    private static Map<String, Object> toMap(FileConfiguration config) {
        Map<String, Object> root = new HashMap<>();
        root.put("panel-url", config.getString("panel-url", ""));
        root.put("server-id", config.getString("server-id", ""));
        root.put("token", config.getString("token", ""));
        root.put("panel-insecure-ssl", config.getBoolean("panel-insecure-ssl", false));
        ConfigurationSection control = config.getConfigurationSection("control");
        Map<String, Object> controlMap = new HashMap<>();
        if (control != null) {
            controlMap.put("enabled", control.getBoolean("enabled", false));
            controlMap.put("bind", control.getString("bind", "127.0.0.1"));
            controlMap.put("port", control.getInt("port", 25580));
            controlMap.put("token", control.getString("token", ""));
        }
        root.put("control", controlMap);
        return root;
    }
}
