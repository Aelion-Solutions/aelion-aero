package com.aelion.aero.common.command;

import com.aelion.aero.common.Permissions;
import com.aelion.aero.common.api.CreateServerRequest;
import com.aelion.aero.common.api.CreateServerResponse;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelHealthResponse;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.control.BackendEntry;
import com.aelion.aero.common.util.Strings;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared /ae (proxy) and /aes (backend) subcommand logic for Paper, Velocity, and Bungee.
 */
public final class AeroCommandService {

    public interface Platform {
        void send(String legacyLine);

        void sendAll(List<String> legacyLines);

        boolean hasPermission(String permission);

        void runAsync(Runnable task);

        void runSync(Runnable task);

        AeroConfig config();

        void reloadConfig() throws Exception;

        /** True for Velocity/Bungee; false for Paper/standalone. */
        default boolean isProxy() {
            return false;
        }

        /** Live proxy backends; empty on non-proxy platforms. */
        default List<BackendEntry> backendsSnapshot() {
            return Collections.emptyList();
        }
    }

    private AeroCommandService() {
    }

    public static void execute(String[] args, Platform platform) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                platform.sendAll(AeroCommandMessages.help(platform.isProxy()));
                break;
            case "info":
                if (!platform.hasPermission(Permissions.INFO)) {
                    platform.send(AeroCommandMessages.noPermission());
                    return;
                }
                platform.sendAll(AeroCommandMessages.info(platform.config()));
                break;
            case "reload":
                if (!platform.hasPermission(Permissions.ADMIN)) {
                    platform.send(AeroCommandMessages.noPermission());
                    return;
                }
                try {
                    platform.reloadConfig();
                    platform.send(AeroCommandMessages.reloaded());
                } catch (Exception e) {
                    platform.send(AeroCommandMessages.reloadFailed(e.getMessage()));
                }
                break;
            case "ping":
                if (!platform.hasPermission(Permissions.INFO)) {
                    platform.send(AeroCommandMessages.noPermission());
                    return;
                }
                ping(platform);
                break;
            case "servers":
                if (!platform.hasPermission(Permissions.INFO)) {
                    platform.send(AeroCommandMessages.noPermission());
                    return;
                }
                servers(platform, args);
                break;
            case "backends":
                if (!platform.hasPermission(Permissions.INFO)) {
                    platform.send(AeroCommandMessages.noPermission());
                    return;
                }
                backends(platform, args);
                break;
            case "create-server":
                if (!platform.hasPermission(Permissions.CREATE)) {
                    platform.send(AeroCommandMessages.noPermission());
                    return;
                }
                createServer(platform, args);
                break;
            default:
                platform.send(AeroCommandMessages.unknownSubcommand(sub, platform.isProxy()));
                break;
        }
    }

    public static List<String> tabComplete(String[] args) {
        return tabComplete(args, false);
    }

    public static List<String> tabComplete(String[] args, boolean proxy) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            Stream<String> roots = Stream.of("help", "info", "reload", "ping", "servers");
            if (proxy) {
                roots = Stream.concat(roots, Stream.of("backends", "create-server"));
            }
            return roots.filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if ("servers".equals(sub)) {
                return Stream.of("list").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
            if (proxy && "backends".equals(sub)) {
                return Stream.of("list").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && "servers".equals(sub) && "list".equalsIgnoreCase(args[1])) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return Stream.of("--names").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static void ping(Platform platform) {
        AeroConfig config = platform.config();
        if (!config.isPanelConfigured()) {
            platform.send(AeroCommandMessages.panelNotConfigured());
            return;
        }

        platform.send(AeroCommandStyle.info("Pinging panel…"));
        platform.runAsync(() -> {
            String result = pingPanel(config);
            platform.runSync(() -> platform.send(result));
        });
    }

    private static String pingPanel(AeroConfig config) {
        HttpPanelClient client = new HttpPanelClient(config);
        try {
            PanelHealthResponse health = client.ping();
            return AeroCommandMessages.pingOk(health.isOk(), health.getVersion());
        } catch (PanelNotConfiguredException e) {
            return AeroCommandMessages.panelNotConfigured();
        } catch (PanelApiException e) {
            try {
                ServerInfoResponse info = client.getServerInfo();
                return AeroCommandMessages.pingReachable(info.getName(), info.getStatus());
            } catch (PanelApiException nested) {
                if (nested.statusCode() <= 0) {
                    return AeroCommandMessages.pingFailed(nested.getMessage());
                }
                return AeroCommandMessages.pingHttpError(nested.statusCode(), nested.responseBody());
            }
        } catch (RuntimeException e) {
            return AeroCommandMessages.pingFailed(e.getMessage());
        }
    }

    private static void servers(Platform platform, String[] args) {
        if (args.length < 2 || !"list".equalsIgnoreCase(args[1])) {
            platform.send(AeroCommandMessages.serversUsage(platform.isProxy()));
            return;
        }
        boolean namesOnly = false;
        for (int i = 2; i < args.length; i++) {
            if ("--names".equalsIgnoreCase(args[i]) || "names".equalsIgnoreCase(args[i])) {
                namesOnly = true;
            }
        }
        if (!platform.config().isPanelConfigured()) {
            platform.send(AeroCommandMessages.panelNotConfigured());
            return;
        }

        platform.send(AeroCommandStyle.info("Loading servers…"));
        final boolean names = namesOnly;
        platform.runAsync(() -> {
            List<String> lines;
            try {
                List<ServerInfoResponse> servers = new HttpPanelClient(platform.config()).listServers();
                lines = AeroCommandMessages.formatServerList(servers, names);
            } catch (PanelNotConfiguredException e) {
                lines = Collections.singletonList(AeroCommandMessages.panelNotConfigured());
            } catch (PanelApiException e) {
                lines = Collections.singletonList(
                        AeroCommandMessages.pingHttpError(e.statusCode(), e.responseBody()));
            } catch (RuntimeException e) {
                lines = Collections.singletonList(
                        AeroCommandStyle.error("List failed: " + e.getMessage()));
            }
            final List<String> out = lines;
            platform.runSync(() -> platform.sendAll(out));
        });
    }

    private static void backends(Platform platform, String[] args) {
        if (!platform.isProxy()) {
            platform.send(AeroCommandMessages.proxyOnly("backends"));
            return;
        }
        if (args.length >= 2
                && !"list".equalsIgnoreCase(args[1])
                && !Strings.isBlank(args[1])) {
            platform.send(AeroCommandMessages.backendsUsage());
            return;
        }
        platform.sendAll(AeroCommandMessages.formatBackendList(platform.backendsSnapshot()));
    }

    private static void createServer(Platform platform, String[] args) {
        if (!platform.isProxy()) {
            platform.send(AeroCommandMessages.proxyOnly("create-server"));
            return;
        }

        Map<String, String> kv = parseKeyValues(args, 1);
        String name = kv.get("name");
        String template = firstNonBlank(kv.get("template"), kv.get("templatename"));
        String software = kv.get("software");
        String version = kv.get("version");

        if (Strings.isBlank(name)) {
            platform.send(AeroCommandMessages.createUsageServer());
            return;
        }

        boolean hasTemplate = Strings.isNotBlank(template);
        boolean hasSoftware = Strings.isNotBlank(software);
        boolean hasVersion = Strings.isNotBlank(version);

        if (hasTemplate && (hasSoftware || hasVersion)) {
            platform.send(AeroCommandStyle.error("Use either template= OR software=+version=, not both."));
            return;
        }
        if (!hasTemplate && !(hasSoftware && hasVersion)) {
            platform.send(AeroCommandMessages.createUsageServer());
            return;
        }

        if (!platform.config().isPanelConfigured()) {
            platform.send(AeroCommandMessages.panelNotConfigured());
            return;
        }

        CreateServerRequest req = new CreateServerRequest();
        req.setName(name);
        if (hasTemplate) {
            req.setTemplate(template);
        } else {
            req.setSoftware(software);
            req.setVersion(version);
        }
        if (kv.containsKey("memory")) {
            try {
                req.setMemory(Integer.parseInt(kv.get("memory")));
            } catch (NumberFormatException e) {
                platform.send(AeroCommandStyle.error("Invalid memory value."));
                return;
            }
        }
        if (kv.containsKey("nodeid")) {
            req.setNodeId(kv.get("nodeid"));
        } else if (kv.containsKey("nodeId")) {
            req.setNodeId(kv.get("nodeId"));
        }
        if (kv.containsKey("role")) {
            req.setRole(kv.get("role"));
        }
        if (kv.containsKey("autostart")) {
            req.setAutoStart(parseBoolean(kv.get("autostart")));
        } else if (kv.containsKey("autoStart")) {
            req.setAutoStart(parseBoolean(kv.get("autoStart")));
        } else {
            req.setAutoStart(true);
        }

        platform.send(AeroCommandStyle.info("Creating server…"));
        platform.runAsync(() -> {
            String result;
            try {
                CreateServerResponse created = new HttpPanelClient(platform.config()).createServer(req);
                result = AeroCommandMessages.createServerOk(
                        nullToDash(created.getId()),
                        nullToDash(created.getName()),
                        nullToDash(created.getStatus()));
            } catch (PanelNotConfiguredException e) {
                result = AeroCommandMessages.panelNotConfigured();
            } catch (PanelApiException e) {
                result = AeroCommandMessages.pingHttpError(e.statusCode(), e.responseBody());
            } catch (RuntimeException e) {
                result = AeroCommandStyle.error("Create failed: " + e.getMessage());
            }
            final String message = result;
            platform.runSync(() -> platform.send(message));
        });
    }

    private static Boolean parseBoolean(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(v) || "yes".equals(v) || "1".equals(v)) {
            return true;
        }
        if ("false".equals(v) || "no".equals(v) || "0".equals(v)) {
            return false;
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (Strings.isNotBlank(a)) {
            return a;
        }
        if (Strings.isNotBlank(b)) {
            return b;
        }
        return null;
    }

    private static Map<String, String> parseKeyValues(String[] args, int startIndex) {
        Map<String, String> map = new HashMap<>();
        for (int i = startIndex; i < args.length; i++) {
            String part = args[i];
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = part.substring(eq + 1).trim();
            map.put(key, value);
            map.put(part.substring(0, eq).trim(), value);
        }
        return map;
    }

    private static String nullToDash(String value) {
        return Strings.isBlank(value) ? "-" : value;
    }
}
