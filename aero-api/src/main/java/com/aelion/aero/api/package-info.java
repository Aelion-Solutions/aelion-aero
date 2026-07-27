/**
 * Public fleet bridge for first-party plugins that run alongside Aero on a backend.
 *
 * <p>This module is the thin Maven artifact {@code com.aelion.aero:aero-api}. It has
 * <strong>no</strong> Minecraft or panel-client dependencies. Sibling plugins (Signs, NPCs, …)
 * depend on it at {@code compileOnly} and look up the live implementation at runtime via
 * Bukkit {@code ServicesManager}.
 *
 * <h2>Runtime lookup</h2>
 * Every backend Aero band registers an {@link com.aelion.aero.api.AeroFleetService} on enable:
 * <pre>{@code
 * RegisteredServiceProvider&lt;AeroFleetService&gt; rsp =
 *         Bukkit.getServicesManager().getRegistration(AeroFleetService.class);
 * if (rsp == null) {
 *     // Aero not installed or not enabled
 *     return;
 * }
 * AeroFleetService fleet = rsp.getProvider();
 * }</pre>
 *
 * <p>Do <strong>not</strong> shade this package into consumer plugins — types must match the
 * classes loaded from the Aero plugin JAR.
 *
 * <h2>Player utils</h2>
 * {@link com.aelion.aero.api.AeroFleetService} also exposes:
 * <ul>
 *   <li>{@code kickPlayer(uuid, message)} — local kick (blank message → default reason)</li>
 *   <li>{@code transferToServer(uuid, idOrName)} — resolve panel fleet server → BungeeCord Connect</li>
 *   <li>{@code transferToGroup(uuid, idOrName)} — joinable member with lowest player count → Connect</li>
 * </ul>
 * Operators can run the same actions via {@code /aes kick} / {@code /aes transfer}, or the panel
 * can drive them through the localhost control API ({@code POST /v1/players/kick|transfer}).
 *
 * <h2>What this API is not</h2>
 * <ul>
 *   <li>Not the panel REST client (that lives in {@code aero-common}).</li>
 *   <li>Not the proxy control plane ({@code PUT /v1/backends}).</li>
 *   <li>Not available on Velocity/Bungee — fleet bridge is backend-only.</li>
 * </ul>
 *
 * @see com.aelion.aero.api.AeroFleetService
 * @see com.aelion.aero.api.FleetServerSnapshot
 * @see com.aelion.aero.api.FleetGroupSnapshot
 */
package com.aelion.aero.api;
