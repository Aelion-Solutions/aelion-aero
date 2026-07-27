package com.aelion.aero.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable snapshot of one server group from the panel Aero API.
 *
 * <p>Produced by {@link AeroFleetService#listGroups()}. Aggregates player counts and
 * embeds per-member {@link FleetServerSnapshot}s (member rows may omit {@code status} /
 * {@code software}).
 *
 * <p>Thread-safe: fields are final; {@link #members()} is an unmodifiable copy.
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

    /**
     * Creates a group snapshot.
     *
     * <p>Callers outside Aero normally obtain instances from {@link AeroFleetService}.
     * {@code members} is defensively copied; a {@code null} list becomes empty.
     *
     * @param id             panel group id
     * @param name           display name
     * @param status         provisioned group status from the panel
     * @param currentPlayers aggregate players across members (panel view)
     * @param maxPlayers     aggregate max players (panel view)
     * @param memberCount    number of member servers (may differ from {@code members.size()}
     *                       if the panel truncates the member list)
     * @param liveStatus     aggregate live status from the panel
     * @param members        member server snapshots; {@code null} treated as empty
     */
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

    /**
     * Panel group id.
     *
     * @return id, or {@code null} if omitted
     */
    public String id() {
        return id;
    }

    /**
     * Display name of the group.
     *
     * @return name, or {@code null} if omitted
     */
    public String name() {
        return name;
    }

    /**
     * Provisioned status string from the panel.
     *
     * @return status, or {@code null}/{@code ""} when unknown
     */
    public String status() {
        return status;
    }

    /**
     * Aggregate current players across the group (panel view).
     *
     * @return non-negative count
     */
    public int currentPlayers() {
        return currentPlayers;
    }

    /**
     * Aggregate max players across the group (panel view).
     *
     * @return max players, or {@code 0} if unknown
     */
    public int maxPlayers() {
        return maxPlayers;
    }

    /**
     * Number of member servers according to the panel.
     *
     * @return member count (may exceed {@link #members()}{@code size()} if truncated)
     */
    public int memberCount() {
        return memberCount;
    }

    /**
     * Aggregate live status for the group (e.g. online / searching).
     *
     * @return live status, or {@code null} if omitted
     */
    public String liveStatus() {
        return liveStatus;
    }

    /**
     * Member server snapshots included in this group response.
     *
     * @return unmodifiable list, never {@code null} (may be empty)
     */
    public List<FleetServerSnapshot> members() {
        return members;
    }

    /**
     * Placeholder group used when a consumer needs an id/name before the first successful poll.
     *
     * <p>{@code liveStatus} is {@code "searching"}; counts are zero; members are empty.
     *
     * @param id   group id to advertise
     * @param name display name to advertise
     * @return empty placeholder snapshot
     */
    public static FleetGroupSnapshot empty(String id, String name) {
        return new FleetGroupSnapshot(id, name, "", 0, 0, 0, "searching", Collections.emptyList());
    }
}
