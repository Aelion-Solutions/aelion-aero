package com.aelion.aero.api;

/**
 * One fleet server from the panel Aero API.
 * Keep FQCN in sync with aelion-cloud-plugins copies.
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
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    public String software() {
        return software;
    }

    public String liveStatus() {
        return liveStatus;
    }

    public int currentPlayers() {
        return currentPlayers;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public String groupId() {
        return groupId;
    }

    public String groupName() {
        return groupName;
    }

    public boolean joinable() {
        return joinable;
    }

    public String proxyName() {
        return proxyName;
    }
}
