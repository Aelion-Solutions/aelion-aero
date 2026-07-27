# Aero compatibility matrix

Source of truth: [`compat/aero-compat.yml`](../compat/aero-compat.yml).

Aero ships **one JAR per platform version band**, not one JAR per Minecraft patch.
Product version (e.g. `0.2.0`) is independent of the Minecraft/proxy API band.

## Backend bands

| Artifact | MC range | Compile API | JVM bytecode |
|----------|----------|-------------|--------------|
| `aero-bukkit-1_8` | 1.8 – 1.12.2 | Spigot 1.8.8 | Java 8 |
| `aero-bukkit-1_13` | 1.13 – 1.16.5 | Spigot 1.13.2 | Java 8 |
| `aero-paper-1_17` | 1.17 – 1.20.x | Paper 1.17.1 | Java 16 |
| `aero-paper-1_21` | 1.21+ | Paper 1.21.x | Java 21 |

All backend bands include:

- Config inject / reload
- `/ae` core commands (`help`, `info`, `reload`, `ping`, `servers list`)
- `AeroFleetService` via Bukkit `ServicesManager`
- BungeeCord `Connect` plugin messaging

`aero-paper-1_21` additionally registers `/ae` via Paper Brigadier lifecycle events.

**Deprecated:** the old release asset name `aero-paper-<version>.jar` is replaced by `aero-paper-1_21-<version>.jar`.

## Proxy artifacts

| Artifact | Software | Notes |
|----------|----------|-------|
| `aero-velocity` | Velocity 3.4.x | Live backend registry + join routing |
| `aero-bungee` | BungeeCord / Waterfall | Live backend registry + join routing |

Proxies are **not** banded by Minecraft client/server version.

## Cloud selection algorithm

On provision / plugin install:

1. Map instance `software` to a family (`paper`, `spigot`, `velocity`, …).
2. Parse MC or proxy API version.
3. Find the **unique** matching row in `compat/aero-compat.yml`.
4. Install `data/aero-plugins/<productVer>/<artifact>-<productVer>.jar`.
5. If no row matches → **fail provision** with a clear unsupported error (do not guess).

See also [CLOUD_INTEGRATION.md](CLOUD_INTEGRATION.md).
