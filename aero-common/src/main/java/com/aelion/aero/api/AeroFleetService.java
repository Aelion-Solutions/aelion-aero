package com.aelion.aero.api;

/**
 * Shared fleet bridge for first-party plugins (Signs, NPCs, …).
 * Paper registers an implementation on Bukkit ServicesManager.
 *
 * <p>Keep this FQCN in sync with aelion-cloud-plugins {@code plugins-common}
 * copies when the API changes.
 */
public interface AeroFleetService {

    boolean isConfigured();

    /**
     * Force a panel refresh (otherwise a short TTL cache is used).
     */
    void refresh();

    java.util.List<FleetServerSnapshot> listServers();

    java.util.List<FleetGroupSnapshot> listGroups();

    /**
     * Request a proxy transfer for the given player UUID to {@code proxyServerName}.
     * @return false if the player is offline or messaging failed
     */
    boolean connectPlayer(java.util.UUID playerId, String proxyServerName);
}
