package com.aelion.aero.common.fleet;

import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Diffs fleet snapshots into short operator-facing notify lines (no prefix).
 */
public final class FleetNotifyDiff {

    private static final Set<String> NOTIFY_STATUSES;

    static {
        Set<String> statuses = new HashSet<>();
        statuses.add("starting");
        statuses.add("running");
        statuses.add("stopping");
        statuses.add("stopped");
        statuses.add("restarting");
        statuses.add("crashed");
        statuses.add("error");
        statuses.add("provisioning");
        NOTIFY_STATUSES = Collections.unmodifiableSet(statuses);
    }

    private FleetNotifyDiff() {
    }

    /**
     * @param previous   previous baseline (may be empty for first poll — caller should skip notify)
     * @param current    current fleet rows keyed by server id
     * @param skipServerId this plugin's own server id (never notify about self)
     * @return bare message bodies (no {@code [Aero]} prefix), in stable order
     */
    public static List<String> diff(
            Map<String, FleetNotifySnapshot> previous,
            Map<String, FleetNotifySnapshot> current,
            String skipServerId
    ) {
        Map<String, FleetNotifySnapshot> prev = previous == null
                ? Collections.emptyMap()
                : previous;
        Map<String, FleetNotifySnapshot> curr = current == null
                ? Collections.emptyMap()
                : current;
        String skip = skipServerId == null ? "" : skipServerId.trim();

        List<String> lines = new ArrayList<>();

        for (Map.Entry<String, FleetNotifySnapshot> entry : curr.entrySet()) {
            String id = entry.getKey();
            if (Strings.isBlank(id) || id.equals(skip)) {
                continue;
            }
            FleetNotifySnapshot now = entry.getValue();
            FleetNotifySnapshot before = prev.get(id);
            if (before == null) {
                String verb = verbForStatus(now.status(), true);
                if (verb != null) {
                    lines.add(formatLine(now.name(), verb, now.scaled(), now.groupName()));
                }
                continue;
            }
            if (before.status().equals(now.status())) {
                continue;
            }
            if (!NOTIFY_STATUSES.contains(now.status())) {
                continue;
            }
            String verb = verbForStatus(now.status(), false);
            if (verb != null) {
                lines.add(formatLine(now.name(), verb, now.scaled(), now.groupName()));
            }
        }

        for (Map.Entry<String, FleetNotifySnapshot> entry : prev.entrySet()) {
            String id = entry.getKey();
            if (Strings.isBlank(id) || id.equals(skip)) {
                continue;
            }
            if (!curr.containsKey(id)) {
                FleetNotifySnapshot gone = entry.getValue();
                lines.add(formatLine(gone.name(), "removed", gone.scaled(), gone.groupName()));
            }
        }

        return lines;
    }

    /**
     * Builds an ordered map from panel server rows. Prefers {@code liveStatus}, then {@code status}.
     */
    public static Map<String, FleetNotifySnapshot> index(
            Iterable<? extends FleetNotifyServerView> servers
    ) {
        Map<String, FleetNotifySnapshot> map = new LinkedHashMap<>();
        if (servers == null) {
            return map;
        }
        for (FleetNotifyServerView server : servers) {
            if (server == null || Strings.isBlank(server.id())) {
                continue;
            }
            String status = Strings.isNotBlank(server.liveStatus())
                    ? server.liveStatus()
                    : server.status();
            map.put(
                    server.id().trim(),
                    new FleetNotifySnapshot(
                            server.id(),
                            server.name(),
                            status,
                            server.groupId(),
                            server.groupName()));
        }
        return map;
    }

    public static String verbForStatus(String status, boolean appeared) {
        String s = FleetNotifySnapshot.normalizeStatus(status);
        if (Strings.isBlank(s)) {
            return appeared ? "started" : null;
        }
        switch (s) {
            case "starting":
            case "provisioning":
                return "starting";
            case "running":
                return "started";
            case "stopping":
                return "stopping";
            case "stopped":
                return "stopped";
            case "restarting":
                return "restarting";
            case "crashed":
                return "crashed";
            case "error":
                return "error";
            default:
                return appeared ? "started" : null;
        }
    }

    public static String formatLine(String name, String verb, boolean scaled, String groupName) {
        StringBuilder sb = new StringBuilder();
        sb.append(Strings.isBlank(name) ? "-" : name);
        sb.append(' ');
        sb.append(verb);
        if (scaled) {
            sb.append(" · scaled");
            if (Strings.isNotBlank(groupName)) {
                sb.append(" · ").append(groupName);
            }
        }
        return sb.toString();
    }

    /**
     * Minimal server fields for indexing (avoids coupling tests to Jackson DTOs).
     */
    public interface FleetNotifyServerView {
        String id();

        String name();

        String status();

        String liveStatus();

        String groupId();

        String groupName();
    }
}
