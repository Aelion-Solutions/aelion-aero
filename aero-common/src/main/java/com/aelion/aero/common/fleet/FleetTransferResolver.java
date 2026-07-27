package com.aelion.aero.common.fleet;

import com.aelion.aero.api.FleetGroupSnapshot;
import com.aelion.aero.api.FleetServerSnapshot;
import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure helpers to resolve fleet transfer targets (server / group → proxy name).
 */
public final class FleetTransferResolver {

    private FleetTransferResolver() {
    }

    /**
     * Find a fleet server by panel id or display name (case-insensitive name).
     *
     * @return matching snapshot, or {@code null}
     */
    public static FleetServerSnapshot findServer(List<FleetServerSnapshot> servers, String idOrName) {
        if (servers == null || Strings.isBlank(idOrName)) {
            return null;
        }
        String needle = idOrName.trim();
        String needleLower = needle.toLowerCase(Locale.ROOT);
        for (FleetServerSnapshot s : servers) {
            if (s == null) {
                continue;
            }
            if (needle.equals(s.id())) {
                return s;
            }
            if (s.name() != null && needleLower.equals(s.name().toLowerCase(Locale.ROOT))) {
                return s;
            }
        }
        return null;
    }

    /**
     * Find a fleet group by panel id or display name (case-insensitive name).
     *
     * @return matching snapshot, or {@code null}
     */
    public static FleetGroupSnapshot findGroup(List<FleetGroupSnapshot> groups, String idOrName) {
        if (groups == null || Strings.isBlank(idOrName)) {
            return null;
        }
        String needle = idOrName.trim();
        String needleLower = needle.toLowerCase(Locale.ROOT);
        for (FleetGroupSnapshot g : groups) {
            if (g == null) {
                continue;
            }
            if (needle.equals(g.id())) {
                return g;
            }
            if (g.name() != null && needleLower.equals(g.name().toLowerCase(Locale.ROOT))) {
                return g;
            }
        }
        return null;
    }

    /**
     * Pick a joinable group member with the lowest {@code currentPlayers};
     * ties broken by display name (then id).
     *
     * @return chosen member, or {@code null} if none joinable
     */
    public static FleetServerSnapshot pickJoinableMember(FleetGroupSnapshot group) {
        if (group == null || group.members() == null || group.members().isEmpty()) {
            return null;
        }
        List<FleetServerSnapshot> joinable = new ArrayList<>();
        for (FleetServerSnapshot m : group.members()) {
            if (m != null && m.joinable()) {
                joinable.add(m);
            }
        }
        if (joinable.isEmpty()) {
            return null;
        }
        joinable.sort(Comparator
                .comparingInt(FleetServerSnapshot::currentPlayers)
                .thenComparing(s -> s.name() == null ? "" : s.name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.id() == null ? "" : s.id()));
        return joinable.get(0);
    }

    /**
     * Proxy-registered name for Connect, falling back to display name.
     */
    public static String proxyNameOf(FleetServerSnapshot server) {
        if (server == null) {
            return null;
        }
        if (Strings.isNotBlank(server.proxyName())) {
            return server.proxyName().trim();
        }
        return Strings.isBlank(server.name()) ? null : server.name().trim();
    }
}
