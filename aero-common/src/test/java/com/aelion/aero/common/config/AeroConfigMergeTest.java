package com.aelion.aero.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AeroConfigMergeTest {

    @TempDir
    Path temp;

    @Test
    void identityOverlaysOperatorYaml() throws Exception {
        Files.write(
                temp.resolve("config.yml"),
                "config-version: 1\n".getBytes(StandardCharsets.UTF_8));
        Files.write(
                AeroIdentity.pathIn(temp),
                ("{\n"
                        + "  \"panelUrl\": \"https://panel.local\",\n"
                        + "  \"serverId\": \"srv_1\",\n"
                        + "  \"token\": \"secret\",\n"
                        + "  \"panelInsecureSsl\": true,\n"
                        + "  \"control\": {\n"
                        + "    \"enabled\": true,\n"
                        + "    \"bind\": \"127.0.0.1\",\n"
                        + "    \"port\": 25581,\n"
                        + "    \"token\": \"ctrl\"\n"
                        + "  }\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));

        AeroConfig cfg = AeroConfigLoader.loadMerged(temp);
        assertEquals("https://panel.local", cfg.panelUrl());
        assertEquals("srv_1", cfg.serverId());
        assertEquals("secret", cfg.token());
        assertTrue(cfg.panelInsecureSsl());
        assertTrue(cfg.control().enabled());
        assertEquals(25581, cfg.control().port());
        assertEquals("ctrl", cfg.control().token());
    }

    @Test
    void legacyYamlIdentityUsedWhenAeroAeMissing() throws Exception {
        Files.write(
                temp.resolve("config.yml"),
                ("panel-url: \"https://old.example\"\n"
                        + "server-id: \"legacy\"\n"
                        + "token: \"tok\"\n"
                        + "panel-insecure-ssl: true\n"
                        + "control:\n"
                        + "  enabled: true\n"
                        + "  bind: \"127.0.0.1\"\n"
                        + "  port: 25580\n"
                        + "  token: \"c\"\n").getBytes(StandardCharsets.UTF_8));

        AeroConfig cfg = AeroConfigLoader.loadMerged(temp);
        assertEquals("https://old.example", cfg.panelUrl());
        assertEquals("legacy", cfg.serverId());
        assertTrue(cfg.panelInsecureSsl());
        assertTrue(cfg.control().enabled());
    }

    @Test
    void migratorExtractsIdentityAndStripsYaml() throws Exception {
        Files.write(
                temp.resolve("config.yml"),
                ("panel-url: \"https://old.example\"\n"
                        + "server-id: \"legacy\"\n"
                        + "token: \"tok\"\n"
                        + "panel-insecure-ssl: true\n"
                        + "control:\n"
                        + "  enabled: true\n"
                        + "  port: 25580\n"
                        + "  token: \"c\"\n").getBytes(StandardCharsets.UTF_8));

        // Use on-disk defaults via a tiny ClassLoader resource — write defaults into temp and
        // load via custom loader that serves our operator default.
        ClassLoader loader = new ClassLoader() {
            @Override
            public java.io.InputStream getResourceAsStream(String name) {
                if ("config.yml".equals(name)) {
                    return new java.io.ByteArrayInputStream(
                            "config-version: 1\n".getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
        };

        ConfigMigrator.Result result = ConfigMigrator.ensureOperatorConfig(
                temp, loader, "config.yml", msg -> {
                });
        assertTrue(Files.isRegularFile(AeroIdentity.pathIn(temp)));
        assertFalse(result.config().containsKey("panel-url"));
        assertFalse(result.config().containsKey("token"));
        assertEquals(1, ((Number) result.config().get("config-version")).intValue());

        AeroConfig cfg = AeroConfigLoader.loadMerged(temp);
        assertEquals("https://old.example", cfg.panelUrl());
        assertEquals("legacy", cfg.serverId());
        assertTrue(cfg.panelInsecureSsl());
    }

    @Test
    void incompleteAeroAePreservesYamlControl() throws Exception {
        Files.write(
                temp.resolve("config.yml"),
                ("config-version: 1\n"
                        + "control:\n"
                        + "  enabled: true\n"
                        + "  bind: \"127.0.0.1\"\n"
                        + "  port: 25580\n"
                        + "  token: \"yaml-ctrl\"\n").getBytes(StandardCharsets.UTF_8));
        Files.write(
                AeroIdentity.pathIn(temp),
                ("{\n"
                        + "  \"panelUrl\": \"https://panel.local\",\n"
                        + "  \"serverId\": \"srv_1\",\n"
                        + "  \"token\": \"secret\"\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));

        AeroConfig cfg = AeroConfigLoader.loadMerged(temp);
        assertEquals("https://panel.local", cfg.panelUrl());
        assertTrue(cfg.control().enabled());
        assertEquals("yaml-ctrl", cfg.control().token());
    }

    @Test
    void migratorDoesNotStripYamlWhenAeroAeAlreadyExists() throws Exception {
        Files.write(
                temp.resolve("config.yml"),
                ("panel-url: \"https://yaml.example\"\n"
                        + "server-id: \"yaml-id\"\n"
                        + "token: \"yaml-tok\"\n"
                        + "control:\n"
                        + "  enabled: true\n"
                        + "  token: \"yaml-ctrl\"\n").getBytes(StandardCharsets.UTF_8));
        Files.write(
                AeroIdentity.pathIn(temp),
                ("{\n"
                        + "  \"panelUrl\": \"https://cloud.example\",\n"
                        + "  \"serverId\": \"cloud-id\",\n"
                        + "  \"token\": \"cloud-tok\"\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));

        ClassLoader loader = new ClassLoader() {
            @Override
            public java.io.InputStream getResourceAsStream(String name) {
                if ("config.yml".equals(name)) {
                    return new java.io.ByteArrayInputStream(
                            "config-version: 1\n".getBytes(StandardCharsets.UTF_8));
                }
                return null;
            }
        };

        ConfigMigrator.Result result = ConfigMigrator.ensureOperatorConfig(
                temp, loader, "config.yml", msg -> {
                });
        assertTrue(result.config().containsKey("panel-url"));
        assertTrue(result.config().containsKey("token"));
        assertTrue(result.config().containsKey("control"));
        assertEquals(1, ((Number) result.config().get("config-version")).intValue());

        AeroConfig cfg = AeroConfigLoader.loadMerged(temp);
        assertEquals("https://cloud.example", cfg.panelUrl());
        assertTrue(cfg.control().enabled());
        assertEquals("yaml-ctrl", cfg.control().token());
    }

    @Test
    void additiveMergeAddsMissingKeysOnly() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("config-version", 1);
        defaults.put("future-knob", "default");

        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("config-version", 1);
        existing.put("future-knob", "user");

        java.util.List<String> added = new java.util.ArrayList<>();
        Map<String, Object> merged = ConfigMigrator.deepMergeMissing(defaults, existing, "", added);
        assertEquals("user", merged.get("future-knob"));
        assertTrue(added.isEmpty());

        Map<String, Object> sparse = new LinkedHashMap<>();
        sparse.put("config-version", 1);
        added.clear();
        Map<String, Object> filled = ConfigMigrator.deepMergeMissing(defaults, sparse, "", added);
        assertEquals("default", filled.get("future-knob"));
        assertEquals(1, added.size());
        assertEquals("future-knob", added.get(0));
    }
}
