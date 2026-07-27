package com.aelion.aero.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Additive + versioned migration for operator {@code config.yml}.
 * Never resets existing user values; may strip legacy identity keys into {@code aero.ae}.
 */
public final class ConfigMigrator {

    public static final int CURRENT_VERSION = 1;
    public static final String VERSION_KEY = "config-version";

    private static final ObjectMapper YAML = new ObjectMapper(
            new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

    private static final String[] IDENTITY_KEYS = {
            "panel-url",
            "server-id",
            "token",
            "panel-insecure-ssl",
            "control"
    };

    private ConfigMigrator() {
    }

    public static final class Result {
        private final Map<String, Object> config;
        private final List<String> addedKeys;
        private final boolean rewritten;

        Result(Map<String, Object> config, List<String> addedKeys, boolean rewritten) {
            this.config = config;
            this.addedKeys = addedKeys;
            this.rewritten = rewritten;
        }

        public Map<String, Object> config() {
            return config;
        }

        public List<String> addedKeys() {
            return addedKeys;
        }

        public boolean rewritten() {
            return rewritten;
        }
    }

    /**
     * Ensure {@code config.yml} exists (copy from resource if missing), merge missing defaults,
     * run version migrations, optionally extract legacy identity into {@code aero.ae}.
     */
    @SuppressWarnings("unchecked")
    public static Result ensureOperatorConfig(
            Path dataDirectory,
            ClassLoader classLoader,
            String resourceName,
            Consumer<String> logInfo
    ) throws IOException {
        Files.createDirectories(dataDirectory);
        Path configPath = dataDirectory.resolve("config.yml");
        Path identityPath = AeroIdentity.pathIn(dataDirectory);

        if (Files.notExists(configPath)) {
            try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
                if (in == null) {
                    throw new IOException("Missing resource: " + resourceName);
                }
                Files.copy(in, configPath);
            }
        }

        Map<String, Object> defaults = loadYamlResource(classLoader, resourceName);
        Map<String, Object> onDisk = loadYamlFile(configPath);
        if (onDisk == null) {
            onDisk = new LinkedHashMap<>();
        }

        // Version comes from the on-disk file only — additive merge must not
        // invent CURRENT_VERSION and skip migrations.
        int version = onDisk.containsKey(VERSION_KEY)
                ? intVal(onDisk.get(VERSION_KEY), 0)
                : 0;

        List<String> added = new ArrayList<>();
        Map<String, Object> merged = deepMergeMissing(defaults, onDisk, "", added);

        boolean changed = !added.isEmpty();

        if (version < 1) {
            AeroIdentity legacy = AeroIdentity.fromLegacyConfigMap(merged);
            boolean migratedIntoAe = false;
            if (legacy.hasAnyIdentityFields() && Files.notExists(identityPath)) {
                AeroIdentity.write(identityPath, legacy);
                migratedIntoAe = true;
                if (logInfo != null) {
                    logInfo.accept("Migrated identity fields from config.yml into " + AeroIdentity.FILE_NAME);
                }
            }
            // Only strip YAML identity after we copied it into aero.ae. If aero.ae already
            // existed (e.g. cloud inject), leave YAML keys so we do not drop secrets that
            // were never written into the identity file.
            if (migratedIntoAe) {
                for (String key : IDENTITY_KEYS) {
                    if (merged.containsKey(key)) {
                        merged.remove(key);
                        changed = true;
                    }
                }
            }
            merged.put(VERSION_KEY, CURRENT_VERSION);
            changed = true;
            version = CURRENT_VERSION;
        }

        if (version < CURRENT_VERSION) {
            merged.put(VERSION_KEY, CURRENT_VERSION);
            changed = true;
        }

        if (changed) {
            writeYaml(configPath, merged);
        }

        if (logInfo != null && !added.isEmpty()) {
            logInfo.accept("Added missing config keys: " + String.join(", ", added));
        }

        return new Result(merged, added, changed);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> deepMergeMissing(
            Map<String, Object> defaults,
            Map<String, Object> existing,
            String prefix,
            List<String> added
    ) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (existing != null) {
            out.putAll(existing);
        }
        if (defaults == null) {
            return out;
        }
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            String key = e.getKey();
            Object defVal = e.getValue();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (!out.containsKey(key)) {
                out.put(key, defVal);
                added.add(path);
                continue;
            }
            Object cur = out.get(key);
            if (defVal instanceof Map && cur instanceof Map) {
                out.put(
                        key,
                        deepMergeMissing(
                                (Map<String, Object>) defVal,
                                (Map<String, Object>) cur,
                                path,
                                added));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYamlFile(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Map<String, Object> root = YAML.readValue(in, Map.class);
            return root == null ? new LinkedHashMap<>() : new LinkedHashMap<>(root);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYamlResource(ClassLoader loader, String resourceName)
            throws IOException {
        try (InputStream in = loader.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing resource: " + resourceName);
            }
            Map<String, Object> root = YAML.readValue(in, Map.class);
            return root == null ? new LinkedHashMap<>() : new LinkedHashMap<>(root);
        }
    }

    private static void writeYaml(Path path, Map<String, Object> root) throws IOException {
        YAML.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), root);
    }

    private static int intVal(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(Objects.toString(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
