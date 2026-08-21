package com.aelion.aero.common.control;

import com.aelion.aero.common.util.Strings;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * One proxy backend target.
 *
 * Optional group CT fields ({@code groupId}, {@code playerDistribution}) drive
 * initial lobby selection on the proxy. {@code joinable} / capacity fields are
 * soft hints for lobbies today; game/minigame plugins may later override
 * soft-full and premium join rules via Aero control APIs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BackendEntry {

    private String name;
    private String address;
    private ProxyBackendRole role = ProxyBackendRole.BACKEND;
    private String groupId;
    private String playerDistribution;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private Boolean joinable;

    public BackendEntry() {
    }

    public BackendEntry(String name, String address, ProxyBackendRole role) {
        this.name = name;
        this.address = address;
        this.role = role == null ? ProxyBackendRole.BACKEND : role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ProxyBackendRole getRole() {
        return role;
    }

    public void setRole(ProxyBackendRole role) {
        this.role = role == null ? ProxyBackendRole.BACKEND : role;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getPlayerDistribution() {
        return playerDistribution;
    }

    public void setPlayerDistribution(String playerDistribution) {
        this.playerDistribution = playerDistribution;
    }

    public int getMaxPlayers() {
        return maxPlayers == null ? 0 : maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers == null ? 0 : currentPlayers;
    }

    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public boolean isJoinable() {
        return Boolean.TRUE.equals(joinable);
    }

    public boolean hasJoinable() {
        return joinable != null;
    }

    public void setJoinable(Boolean joinable) {
        this.joinable = joinable;
    }

    public boolean isValid() {
        return Strings.isNotBlank(name) && Strings.isNotBlank(address);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BackendEntry)) {
            return false;
        }
        BackendEntry that = (BackendEntry) o;
        return Objects.equals(name, that.name)
                && Objects.equals(address, that.address)
                && role == that.role
                && Objects.equals(groupId, that.groupId)
                && Objects.equals(playerDistribution, that.playerDistribution)
                && Objects.equals(maxPlayers, that.maxPlayers)
                && Objects.equals(currentPlayers, that.currentPlayers)
                && Objects.equals(joinable, that.joinable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, address, role, groupId, playerDistribution, maxPlayers, currentPlayers, joinable);
    }
}
