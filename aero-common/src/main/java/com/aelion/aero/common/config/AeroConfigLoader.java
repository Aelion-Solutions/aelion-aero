package com.aelion.aero.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Loads {@link AeroConfig} from operator {@code config.yml} + identity {@code aero.ae}.
 */
public final class AeroConfigLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private AeroConfigLoader() {
    }

    /**
     * Migrate operator config if needed, then merge {@code config.yml} + {@code aero.ae}.
     */
    public static AeroConfig loadDataDirectory(
            Path dataDirectory,
            ClassLoader classLoader,
            Consumer<String> logInfo
    ) throws IOException {
        ConfigMigrator.ensureOperatorConfig(dataDirectory, classLoader, "config.yml", logInfo);
        return loadMerged(dataDirectory);
    }

    /**
     * Load without migration (tests / already-prepared dirs).
     */
    public static AeroConfig loadMerged(Path dataDirectory) throws IOException {
        Path configPath = dataDirectory.resolve("config.yml");
        Path identityPath = AeroIdentity.pathIn(dataDirectory);

        AeroConfig fromYaml = AeroConfig.empty();
        if (Files.isRegularFile(configPath)) {
            fromYaml = loadYaml(configPath);
        }

        if (Files.isRegularFile(identityPath)) {
            AeroIdentity identity = AeroIdentity.load(identityPath);
            return identity.applyOnto(fromYaml);
        }
        // Legacy: identity still in config.yml
        return fromYaml;
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
