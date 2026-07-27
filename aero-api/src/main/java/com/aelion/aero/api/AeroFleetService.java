package com.aelion.aero.api;

import java.util.List;
import java.util.UUID;

/**
 * Shared fleet bridge registered by Aero on Bukkit/Paper backends.
 *
 * <p>First-party plugins (Signs, NPCs, …) obtain this service from Bukkit
 * {@code ServicesManager} and must <strong>not</strong> carry their own panel tokens.
 * Aero owns panel credentials and caches fleet polls.
 *
 * <h2>Lifecycle</h2>
 * <ul>
 *   <li>Registered on Aero enable with {@code ServicePriority.Normal}.</li>
 *   <li>Unregistered on Aero disable.</li>
 *   <li>If {@link #isConfigured()} is {@code false}, list methods return empty
 *       collections and {@link #connectPlayer} returns {@code false}.</li>
 * </ul>
 *
 * <h2>Caching</h2>
 * {@link #listServers()} and {@link #listGroups()} use a short TTL cache (about 2&nbsp;s in
 * the stock implementation). Call {@link #refresh()} to force a panel round-trip.
 *
 * <p>Published as {@code com.aelion.aero:aero-api} on GitHub Packages.
 *
 * @see FleetServerSnapshot
 * @see FleetGroupSnapshot
 */
public interface AeroFleetService {

    /**
     * Whether Aero has a usable panel URL, server id, and token.
     *
     * @return {@code true} if fleet queries against the panel can succeed;
     *         {@code false} if config is incomplete (lists will be empty)
     */
    boolean isConfigured();

    /**
     * Force a panel refresh of servers and groups.
     *
     * <p>Otherwise {@link #listServers()} / {@link #listGroups()} reuse a short TTL cache.
     * Failures are logged by Aero; subsequent list calls may still return the last
     * successful snapshot (or empty if none).
     */
    void refresh();

    /**
     * Same-owner fleet servers from the panel Aero API.
     *
     * <p>May trigger a cached refresh. The returned list is unmodifiable; elements are
     * immutable snapshots and must not be mutated.
     *
     * @return fleet servers, never {@code null} (empty when unconfigured or on error)
     */
    List<FleetServerSnapshot> listServers();

    /**
     * Same-owner server groups (with member snapshots) from the panel Aero API.
     *
     * <p>May trigger a cached refresh. The returned list is unmodifiable; member lists
     * inside each group are also unmodifiable.
     *
     * @return fleet groups, never {@code null} (empty when unconfigured or on error)
     */
    List<FleetGroupSnapshot> listGroups();

    /**
     * Request a proxy transfer for an online player to the named proxy backend.
     *
     * <p>Uses the BungeeCord plugin-messaging {@code Connect} subchannel (also accepted by
     * Velocity’s legacy BungeeCord channel). {@code proxyServerName} should be the name
     * registered on the proxy — typically {@link FleetServerSnapshot#proxyName()}, not the
     * panel display name when those differ.
     *
     * @param playerId        Bukkit player UUID; must be online on this backend
     * @param proxyServerName server name as known to Velocity/Bungee (trimmed); blank is rejected
     * @return {@code true} if the Connect message was sent;
     *         {@code false} if the player is offline, arguments are invalid, or messaging failed
     */
    boolean connectPlayer(UUID playerId, String proxyServerName);
}
