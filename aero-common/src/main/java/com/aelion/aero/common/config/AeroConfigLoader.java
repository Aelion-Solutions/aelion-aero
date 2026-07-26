package com.aelion.aero.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads {@link AeroConfig} from YAML (Velocity data dir / shared tests).
 */
public final class AeroConfigLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private AeroConfigLoader() {
    }

    @SuppressWarnings("unchecked")
    public static AeroConfig loadYaml(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Map<String, Object> root = YAML.readValue(in, Map.class);
            return AeroConfig.fromMap(root);
        }
    }

    @SuppressWarnings("unchecked")
    public static AeroConfig loadYamlResource(ClassLoader loader, String resourceName) throws IOException {
        try (InputStream in = loader.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing resource: " + resourceName);
            }
            Map<String, Object> root = YAML.readValue(in, Map.class);
            return AeroConfig.fromMap(root);
        }
    }
}
