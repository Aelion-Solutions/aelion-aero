package com.aelion.aero.common.control;

import com.aelion.aero.api.FleetGroupSnapshot;
import com.aelion.aero.api.FleetServerSnapshot;
import com.aelion.aero.common.api.GroupInfoResponse;
import com.aelion.aero.common.api.HttpPanelClient;
import com.aelion.aero.common.api.PanelApiException;
import com.aelion.aero.common.api.PanelNotConfiguredException;
import com.aelion.aero.common.api.ServerInfoResponse;
import com.aelion.aero.common.config.AeroConfig;
import com.aelion.aero.common.fleet.FleetTransferResolver;
import com.aelion.aero.common.util.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resolves a transfer target proxy name from a control request + optional fleet/panel data.
 */
public final class ControlTransferResolver {

    private ControlTransferResolver() {
    }

    public static final class Result {
        private final String proxyServerName;
        private final String error;

        private Result(String proxyServerName, String error) {
            this.proxyServerName = proxyServerName;
            this.error = error;
        }

        public static Result ok(String proxyServerName) {
            return new Result(proxyServerName, null);
        }

        public static Result error(String error) {
            return new Result(null, error);
        }

        public boolean isOk() {
            return Strings.isNotBlank(proxyServerName);
        }

        public String proxyServerName() {
            return proxyServerName;
        }

        public String error() {
            return error;
        }
    }

    /**
     * Resolve transfer destination. If {@code proxyServerName} is set, uses it.
     * Otherwise resolves via provided fleet lists or live panel fetch when configured.
     */
    public static Result resolve(
            ControlTransferRequest req,
            AeroConfig config,
            List<FleetServerSnapshot> knownServers,
            List<FleetGroupSnapshot> knownGroups,
            List<String> registryBackendNames
    ) {
        if (req == null || Strings.isBlank(req.getUuid())) {
            return Result.error("uuid is required");
        }
        if (Strings.isNotBlank(req.getProxyServerName())) {
            return Result.ok(req.getProxyServerName().trim());
        }

        if (registryBackendNames != null) {
            String byName = firstNonBlank(req.getServerName(), req.getServerId());
            if (Strings.isNotBlank(byName)) {
                for (String name : registryBackendNames) {
                    if (name != null && name.equalsIgnoreCase(byName.trim())) {
                        return Result.ok(name);
                    }
                }
            }
        }

        List<FleetServerSnapshot> servers = knownServers == null
                ? Collections.<FleetServerSnapshot>emptyList()
                : knownServers;
        List<FleetGroupSnapshot> groups = knownGroups == null
                ? Collections.<FleetGroupSnapshot>emptyList()
                : knownGroups;

        if ((servers.isEmpty() && groups.isEmpty()) && config != null && config.isPanelConfigured()) {
            try {
                HttpPanelClient client = new HttpPanelClient(config);
                servers = mapServers(client.listServers());
                groups = mapGroups(client.listGroups());
            } catch (PanelNotConfiguredException | PanelApiException e) {
                return Result.error("panel fleet unavailable: " + e.getMessage());
            }
        }

        String serverKey = firstNonBlank(req.getServerId(), req.getServerName());
        if (Strings.isNotBlank(serverKey)) {
            FleetServerSnapshot s = FleetTransferResolver.findServer(servers, serverKey);
            String proxy = FleetTransferResolver.proxyNameOf(s);
            if (Strings.isBlank(proxy)) {
                return Result.error("server not found: " + serverKey);
            }
            return Result.ok(proxy);
        }

        String groupKey = firstNonBlank(req.getGroupId(), req.getGroupName());
        if (Strings.isNotBlank(groupKey)) {
            FleetGroupSnapshot g = FleetTransferResolver.findGroup(groups, groupKey);
            FleetServerSnapshot m = FleetTransferResolver.pickJoinableMember(g);
            String proxy = FleetTransferResolver.proxyNameOf(m);
            if (Strings.isBlank(proxy)) {
                return Result.error("no joinable member for group: " + groupKey);
            }
            return Result.ok(proxy);
        }

        return Result.error("proxyServerName or serverId/serverName or groupId/groupName required");
    }

    private static String firstNonBlank(String a, String b) {
        if (Strings.isNotBlank(a)) {
            return a.trim();
        }
        if (Strings.isNotBlank(b)) {
            return b.trim();
        }
        return "";
    }

    private static List<FleetServerSnapshot> mapServers(List<ServerInfoResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }
        List<FleetServerSnapshot> out = new ArrayList<>(responses.size());
        for (ServerInfoResponse r : responses) {
            String proxy = Strings.isBlank(r.getProxyName()) ? r.getName() : r.getProxyName();
            out.add(new FleetServerSnapshot(
                    r.getId(),
                    r.getName(),
                    r.getStatus(),
                    r.getSoftware(),
                    r.getLiveStatus(),
                    r.getCurrentPlayers(),
                    r.getMaxPlayers(),
                    r.getGroupId(),
                    r.getGroupName(),
                    r.isJoinable(),
                    proxy
            ));
        }
        return out;
    }

    private static List<FleetGroupSnapshot> mapGroups(List<GroupInfoResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return Collections.emptyList();
        }
        List<FleetGroupSnapshot> out = new ArrayList<>(responses.size());
        for (GroupInfoResponse g : responses) {
            List<FleetServerSnapshot> members = new ArrayList<>();
            if (g.getMembers() != null) {
                for (GroupInfoResponse.GroupMemberInfo m : g.getMembers()) {
                    String proxy = Strings.isBlank(m.getProxyName()) ? m.getName() : m.getProxyName();
                    members.add(new FleetServerSnapshot(
                            m.getId(),
                            m.getName(),
                            null,
                            null,
                            m.getLiveStatus(),
                            m.getCurrentPlayers(),
                            m.getMaxPlayers(),
                            g.getId(),
                            g.getName(),
                            m.isJoinable(),
                            proxy
                    ));
                }
            }
            out.add(new FleetGroupSnapshot(
                    g.getId(),
                    g.getName(),
                    g.getStatus(),
                    g.getCurrentPlayers(),
                    g.getMaxPlayers(),
                    g.getMemberCount(),
                    g.getLiveStatus(),
                    members
            ));
        }
        return out;
    }
}
