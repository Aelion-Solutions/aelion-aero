package com.aelion.aero.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One server group with members from the panel Aero API.
 */
public final class FleetGroupSnapshot {

    private final String id;
    private final String name;
    private final String status;
    private final int currentPlayers;
    private final int maxPlayers;
    private final int memberCount;
    private final String liveStatus;
    private final List<FleetServerSnapshot> members;

    public FleetGroupSnapshot(
            String id,
            String name,
            String status,
            int currentPlayers,
            int maxPlayers,
            int memberCount,
            String liveStatus,
            List<FleetServerSnapshot> members
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.memberCount = memberCount;
        this.liveStatus = liveStatus;
        this.members = members == null
                ? Collections.<FleetServerSnapshot>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(members));
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

    public int currentPlayers() {
        return currentPlayers;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public int memberCount() {
        return memberCount;
    }

    public String liveStatus() {
        return liveStatus;
    }

    public List<FleetServerSnapshot> members() {
        return members;
    }

    public static FleetGroupSnapshot empty(String id, String name) {
        return new FleetGroupSnapshot(id, name, "", 0, 0, 0, "searching", Collections.emptyList());
    }
}
