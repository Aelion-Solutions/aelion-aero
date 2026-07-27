package com.aelion.aero.common.fleet;

import com.aelion.aero.common.util.Strings;
import java.util.Objects;

/**
 * One fleet server row used for notify diffing.
 */
public final class FleetNotifySnapshot {

    private final String id;
    private final String name;
    private final String status;
    private final String groupName;
    private final boolean scaled;

    public FleetNotifySnapshot(
            String id,
            String name,
            String status,
            String groupId,
            String groupName
    ) {
        this.id = id == null ? "" : id;
        this.name = Strings.isBlank(name) ? this.id : name.trim();
        this.status = normalizeStatus(status);
        this.scaled = Strings.isNotBlank(groupId) || Strings.isNotBlank(groupName);
        this.groupName = Strings.isBlank(groupName)
                ? (Strings.isBlank(groupId) ? "" : groupId.trim())
                : groupName.trim();
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

    public String groupName() {
        return groupName;
    }

    public boolean scaled() {
        return scaled;
    }

    public static String normalizeStatus(String raw) {
        if (Strings.isBlank(raw)) {
            return "";
        }
        return raw.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FleetNotifySnapshot)) {
            return false;
        }
        FleetNotifySnapshot that = (FleetNotifySnapshot) o;
        return scaled == that.scaled
                && Objects.equals(id, that.id)
                && Objects.equals(name, that.name)
                && Objects.equals(status, that.status)
                && Objects.equals(groupName, that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, status, groupName, scaled);
    }
}
