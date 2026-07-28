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
import java.util.UUID;
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

        /**
         * Kick an online player by exact name. Blank message → platform default reason.
         *
         * @return {@code true} if the player was online and kicked
         */
        default boolean kickPlayer(String playerName, String message) {
            return false;
        }

        /**
         * Transfer an online player. Exactly one of {@code serverKey}/{@code groupKey} is set.
         *
         * @return {@code true} if transfer was initiated
         */
        default boolean transferPlayer(String playerName, String serverKey, String groupKey) {
            return false;
        }

        /** Online player names for tab-complete (prefix-filtered by caller). */
        default List<String> onlinePlayerNames() {
            return Collections.emptyList();
        }

        /** Player UUID for the command sender, or {@code null} for console. */
        default UUID senderId() {
            return null;
        }

        /**
         * Enable or disable fleet notify for {@link #senderId()}.
         *
         * @return {@code true} if now enabled
         * @throws UnsupportedOperationException if notify is not wired on this platform
         */
        default boolean setNotifyEnabled(boolean enabled) {
            throw new UnsupportedOperationException("notify");
        }

        /** Whether fleet notify is enabled for {@link #senderId()}. */
        default boolean isNotifyEnabled() {
            return false;
        }

        /** {@code true} when this platform supports fleet notify. */
        default boolean supportsNotify() {
            return false;
        }
    }

    private AeroCommandService() {
    }

    public static void execute(String[] args, Platform platform) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        boolean canInfo = Permissions.allowsInfo(platform::hasPermission);
        boolean canAdmin = Permissions.allowsAdmin(platform::hasPermission);
        boolean canCreate = Permissions.allowsCreate(platform::hasPermission);
        switch (sub) {
            case "help":
                if (!requirePerm(platform, canInfo)) {
                    return;
                }
                platform.sendAll(AeroCommandMessages.help(platform.isProxy(), canInfo, canAdmin, canCreate));
                break;
            case "info":
                if (!requirePerm(platform, canInfo)) {
                    return;
                }
                platform.sendAll(AeroCommandMessages.info(platform.config()));
                break;
            case "reload":
                if (!requirePerm(platform, canAdmin)) {
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
                if (!requirePerm(platform, canInfo)) {
                    return;
                }
                ping(platform);
                break;
            case "servers":
                if (!requirePerm(platform, canInfo)) {
                    return;
                }
                servers(platform, args);
                break;
            case "backends":
                if (!requirePerm(platform, canInfo)) {
                    return;
                }
                backends(platform, args);
                break;
            case "create-server":
                if (!requirePerm(platform, canCreate)) {
                    return;
                }
                createServer(platform, args);
                break;
            case "kick":
                if (!requirePerm(platform, canAdmin)) {
                    return;
                }
                kick(platform, args);
                break;
            case "transfer":
                if (!requirePerm(platform, canAdmin)) {
                    return;
                }
                transfer(platform, args);
                break;
            case "notify":
                if (!requirePerm(platform, canInfo)) {
                    return;
                }
                fleetNotify(platform, args);
                break;
            default:
                platform.send(AeroCommandMessages.unknownSubcommand(sub, platform.isProxy()));
                break;
        }
    }

    private static boolean requirePerm(Platform platform, boolean allowed) {
        if (allowed) {
            return true;
        }
        platform.send(AeroCommandMessages.noPermission());
        return false;
    }

    public static List<String> tabComplete(String[] args) {
        return tabComplete(args, false);
    }

    public static List<String> tabComplete(String[] args, boolean proxy) {
        return tabComplete(args, proxy, Collections.emptyList());
    }

    public static List<String> tabComplete(String[] args, boolean proxy, List<String> onlinePlayers) {
        return tabComplete(args, proxy, onlinePlayers, true, true, true);
    }

    public static List<String> tabComplete(
            String[] args,
            boolean proxy,
            List<String> onlinePlayers,
            boolean canInfo,
            boolean canAdmin,
            boolean canCreate
    ) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            Stream<String> roots = Stream.empty();
            if (canInfo) {
                roots = Stream.of("help", "info", "ping", "servers");
            }
            if (canAdmin) {
                roots = Stream.concat(roots, Stream.of("reload", "kick", "transfer"));
            }
            if (proxy && canInfo) {
                roots = Stream.concat(roots, Stream.of("notify", "backends"));
            }
            if (proxy && canCreate) {
                roots = Stream.concat(roots, Stream.of("create-server"));
            }
            return roots.filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (canInfo && "servers".equals(sub)) {
                return Stream.of("list").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
            if (canInfo && "notify".equals(sub)) {
                return Stream.of("on", "off").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
            if (proxy && canInfo && "backends".equals(sub)) {
                return Stream.of("list").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
            }
            if (canAdmin && ("kick".equals(sub) || "transfer".equals(sub))) {
                List<String> names = onlinePlayers == null ? Collections.emptyList() : onlinePlayers;
                return names.stream()
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .collect(Collectors.toList());
            }
        }
        if (canInfo && args.length == 3 && "servers".equals(sub) && "list".equalsIgnoreCase(args[1])) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return Stream.of("--names").filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        if (canAdmin && args.length >= 3 && "transfer".equals(sub)) {
            String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
            return Stream.of("server=", "group=")
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /** Tab-complete using the caller's live permission set. */
    public static List<String> tabComplete(String[] args, Platform platform, List<String> onlinePlayers) {
        return tabComplete(
                args,
                platform.isProxy(),
                onlinePlayers,
                Permissions.allowsInfo(platform::hasPermission),
                Permissions.allowsAdmin(platform::hasPermission),
                Permissions.allowsCreate(platform::hasPermission));
    }

    private static void fleetNotify(Platform platform, String[] args) {
        if (!platform.isProxy()) {
            platform.send(AeroCommandMessages.proxyOnly("notify"));
            return;
        }
        if (!platform.supportsNotify()) {
            platform.send(AeroCommandMessages.notifyUnavailable());
            return;
        }
        if (platform.senderId() == null) {
            platform.send(AeroCommandMessages.notifyPlayerOnly());
            return;
        }
        Boolean force = null;
        if (args.length >= 2 && Strings.isNotBlank(args[1])) {
            String mode = args[1].trim().toLowerCase(Locale.ROOT);
            if ("on".equals(mode) || "enable".equals(mode) || "true".equals(mode) || "1".equals(mode)) {
                force = true;
            } else if ("off".equals(mode)
                    || "disable".equals(mode)
                    || "false".equals(mode)
                    || "0".equals(mode)) {
                force = false;
            } else {
                platform.send(AeroCommandMessages.notifyUsage(true));
                return;
            }
        }
        boolean enabled;
        try {
            if (force == null) {
                enabled = !platform.isNotifyEnabled();
                platform.setNotifyEnabled(enabled);
            } else {
                enabled = platform.setNotifyEnabled(force);
            }
        } catch (UnsupportedOperationException e) {
            platform.send(AeroCommandMessages.notifyUnavailable());
            return;
        }
        platform.send(AeroCommandMessages.notifyEnabled(enabled));
    }

    private static void kick(Platform platform, String[] args) {
        if (args.length < 2 || Strings.isBlank(args[1])) {
            platform.send(AeroCommandMessages.kickUsage(platform.isProxy()));
            return;
        }
        String playerName = args[1].trim();
        String message = args.length > 2
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                : "";
        boolean ok = platform.kickPlayer(playerName, message);
        if (ok) {
            platform.send(AeroCommandMessages.kickOk(playerName));
        } else {
            platform.send(AeroCommandMessages.playerOffline(playerName));
        }
    }

    private static void transfer(Platform platform, String[] args) {
        if (args.length < 3) {
            platform.send(AeroCommandMessages.transferUsage(platform.isProxy()));
            return;
        }
        String playerName = args[1].trim();
        Map<String, String> kv = parseKeyValues(args, 2);
        String serverKey = firstNonBlank(kv.get("server"), kv.get("serverid"), kv.get("servername"));
        String groupKey = firstNonBlank(kv.get("group"), kv.get("groupid"), kv.get("groupname"));
        boolean hasServer = Strings.isNotBlank(serverKey);
        boolean hasGroup = Strings.isNotBlank(groupKey);
        if (hasServer == hasGroup) {
            platform.send(AeroCommandMessages.transferUsage(platform.isProxy()));
            return;
        }
        boolean ok = platform.transferPlayer(playerName, hasServer ? serverKey : null, hasGroup ? groupKey : null);
        if (ok) {
            platform.send(AeroCommandMessages.transferOk(playerName, hasServer ? serverKey : groupKey));
        } else {
            platform.send(AeroCommandMessages.transferFailed(playerName));
        }
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (Strings.isNotBlank(a)) {
            return a;
        }
        if (Strings.isNotBlank(b)) {
            return b;
        }
        if (Strings.isNotBlank(c)) {
            return c;
        }
        return null;
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
