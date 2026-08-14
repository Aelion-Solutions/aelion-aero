# Aelion Aero

Paper/Spigot, Velocity, and BungeeCord/Waterfall plugins that connect Minecraft to
[Aelion Cloud](https://github.com/Aelion-Solutions/aelion-cloud).

**Running a server with this plugin?** See the [Wiki](https://github.com/Aelion-Solutions/aelion-aero/wiki)
for installation, commands, and configuration.

This README is for building and contributing.

## Modules

| Module | Platform |
|--------|----------|
| `aero-api` | Fleet bridge API for sibling plugins (Maven) |
| `aero-common` | Shared config, DTOs, panel client, commands |
| `aero-bukkit-shared` | Shared Bukkit fleet + classic `/aes` |
| `aero-bukkit-1_8` | Spigot/Paper 1.8–1.12.2 |
| `aero-bukkit-1_13` | Spigot/Paper 1.13–1.16.5 |
| `aero-paper-1_17` | Paper 1.17–1.20.x |
| `aero-paper-1_21` | Paper 1.21.x |
| `aero-paper-26` | Paper 26.x |
| `aero-velocity` | Velocity 3.4.x |
| `aero-bungee` | BungeeCord / Waterfall |

Compatibility matrix: [`compat/aero-compat.yml`](compat/aero-compat.yml), reference: [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Build

Requires JDK 21 (JDK 25 for `aero-paper-26`; Gradle toolchain auto-provisions).

```bash
./gradlew build
```

JARs land in each module's `build/libs/`.

## Docs

- [docs/CLOUD_INTEGRATION.md](docs/CLOUD_INTEGRATION.md) — panel/daemon contract with Aelion Cloud
- [docs/CONTROL_API.md](docs/CONTROL_API.md) — localhost control API reference
- [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) — version bands, artifact matrix, cloud selection
- [docs/RELEASE.md](docs/RELEASE.md) — release-please flow

## License

Proprietary — Aelion Solutions. All rights reserved unless otherwise stated.
