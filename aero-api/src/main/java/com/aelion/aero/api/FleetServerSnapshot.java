package com.aelion.aero.api;

/**
 * Immutable snapshot of one fleet server from the panel Aero API.
 *
 * <p>Produced by {@link AeroFleetService#listServers()} and as members of
 * {@link FleetGroupSnapshot#members()}. Field values mirror panel
 * {@code GET /api/aero/v1/servers} (and group member payloads); unknown or omitted panel
 * fields may be {@code null} or zero.
 *
 * <p>Thread-safe: all fields are final. Prefer {@link #proxyName()} when calling
 * {@link AeroFleetService#connectPlayer(java.util.UUID, String)}.
 */
public final class FleetServerSnapshot {

    private final String id;
    private final String name;
    private final String status;
    private final String software;
    private final String liveStatus;
    private final int currentPlayers;
    private final int maxPlayers;
    private final String groupId;
    private final String groupName;
    private final boolean joinable;
    private final String proxyName;
    private final String motd;

    /**
     * Creates a server snapshot without MOTD (legacy callers).
     *
     * @see #FleetServerSnapshot(String, String, String, String, String, int, int, String, String, boolean, String, String)
     */
    public FleetServerSnapshot(
            String id,
            String name,
            String status,
            String software,
            String liveStatus,
            int currentPlayers,
            int maxPlayers,
            String groupId,
            String groupName,
            boolean joinable,
            String proxyName
    ) {
        this(
                id,
                name,
                status,
                software,
                liveStatus,
                currentPlayers,
                maxPlayers,
                groupId,
                groupName,
                joinable,
                proxyName,
                null
        );
    }

    /**
     * Creates a server snapshot.
     *
     * <p>Callers outside Aero normally obtain instances from {@link AeroFleetService};
     * this constructor exists for the Aero implementation and tests.
     *
     * @param id             panel server id (e.g. {@code cms_…})
     * @param name           display / panel name
     * @param status         provisioned status from the panel (may be {@code null} for group members)
     * @param software       Minecraft software label (may be {@code null} for group members)
     * @param liveStatus     live reachability / runtime status from the panel
     * @param currentPlayers players currently online (panel view)
     * @param maxPlayers     max players (panel view; {@code 0} if unknown)
     * @param groupId        owning group id, or {@code null} if ungrouped
     * @param groupName      owning group display name, or {@code null}
     * @param joinable       whether the panel considers this backend joinable
     * @param proxyName      name registered on the proxy for Connect; Aero falls back to
     *                       {@code name} when the panel omits {@code proxyName}
     * @param motd           live server-list MOTD from the backend (may be {@code null})
     */
    public FleetServerSnapshot(
            String id,
            String name,
            String status,
            String software,
            String liveStatus,
            int currentPlayers,
            int maxPlayers,
            String groupId,
            String groupName,
            boolean joinable,
            String proxyName,
            String motd
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.software = software;
        this.liveStatus = liveStatus;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.groupId = groupId;
        this.groupName = groupName;
        this.joinable = joinable;
        this.proxyName = proxyName;
        this.motd = motd;
    }

    /**
     * Panel server id.
     *
     * @return id, or {@code null} if the panel omitted it
     */
    public String id() {
        return id;
    }

    /**
     * Panel / display name of the server.
     *
     * @return name, or {@code null} if omitted
     */
    public String name() {
        return name;
    }

    /**
     * Provisioned status string from the panel (not the live probe).
     *
     * @return status, or {@code null} (common for group-member rows)
     */
    public String status() {
        return status;
    }

    /**
     * Software label (e.g. paper, velocity).
     *
     * @return software, or {@code null} (common for group-member rows)
     */
    public String software() {
        return software;
    }

    /**
     * Live runtime status from the panel (e.g. running / starting / stopped).
     *
     * @return live status, or {@code null} if omitted
     */
    public String liveStatus() {
        return liveStatus;
    }

    /**
     * Current player count as reported by the panel.
     *
     * @return non-negative count
     */
    public int currentPlayers() {
        return currentPlayers;
    }

    /**
     * Max player slots as reported by the panel.
     *
     * @return max players, or {@code 0} if unknown
     */
    public int maxPlayers() {
        return maxPlayers;
    }

    /**
     * Id of the server group this server belongs to, if any.
     *
     * @return group id, or {@code null}
     */
    public String groupId() {
        return groupId;
    }

    /**
     * Display name of the server group, if any.
     *
     * @return group name, or {@code null}
     */
    public String groupName() {
        return groupName;
    }

    /**
     * Whether the panel marks this backend as joinable (safe for signs / auto-join).
     *
     * @return {@code true} if joinable
     */
    public boolean joinable() {
        return joinable;
    }

    /**
     * Proxy-registered server name for plugin-message {@code Connect}.
     *
     * <p>Use this (not necessarily {@link #name()}) with
     * {@link AeroFleetService#connectPlayer(java.util.UUID, String)}.
     *
     * @return proxy map name; Aero fills this from panel {@code proxyName} or falls back to
     *         {@link #name()}
     */
    public String proxyName() {
        return proxyName;
    }

    /**
     * Live Minecraft server-list MOTD reported by the backend Aero plugin.
     *
     * @return MOTD text, or {@code null} if unknown / not reported yet
     */
    public String motd() {
        return motd;
    }
}
