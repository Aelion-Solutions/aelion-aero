# Aelion Aero

Paper/Spigot, Velocity, and BungeeCord/Waterfall plugins that connect Minecraft to
[Aelion Cloud](https://github.com/Aelion-Solutions/aelion-cloud).

Related: [aelion-cloud#308](https://github.com/Aelion-Solutions/aelion-cloud/issues/308)

## Status (phase 2)

- Proxy `/ae` (+ `/aec`): `help`, `info`, `reload`, `ping`, `servers list`, plus `backends` and `create-server`
- Backend `/aes`: `help`, `info`, `reload`, `ping`, `servers list`, `kick`, `transfer` (Paper 1.21+ uses Brigadier; older bands use classic commands; styled `[Aero]` prefix + colors)
- Shared panel client + create DTOs in `aero-common` (Java 8 bytecode)
- Multi-version **backend bands** from MC 1.8 through 1.21+ — see [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)
- Proxy **live backend registry**: empty on-disk server maps; process memory via `PUT /v1/backends`
- Live join routing (Velocity `PlayerChooseInitialServerEvent` / Bungee `ServerConnectEvent`)
- On backend deregister: move players to a lobby/try target, or disconnect if none
- Cloud wiring documented in [docs/CLOUD_INTEGRATION.md](docs/CLOUD_INTEGRATION.md)

## Modules

| Module | Artifact | Platform |
|--------|----------|----------|
| `aero-api` | (Maven) | Fleet bridge API for sibling plugins |
| `aero-common` | (library) | Config, control DTOs, panel client, commands |
| `aero-bukkit-shared` | (library) | Shared Bukkit fleet + classic `/aes` |
| `aero-bukkit-1_8` | `aero-bukkit-1_8-<version>.jar` | Spigot/Paper **1.8–1.12.2** |
| `aero-bukkit-1_13` | `aero-bukkit-1_13-<version>.jar` | Spigot/Paper **1.13–1.16.5** |
| `aero-paper-1_17` | `aero-paper-1_17-<version>.jar` | Paper **1.17–1.20.x** |
| `aero-paper-1_21` | `aero-paper-1_21-<version>.jar` | Paper **1.21.x** |
| `aero-paper-26` | `aero-paper-26-<version>.jar` | Paper **26.x** |
| `aero-velocity` | `aero-velocity-<version>.jar` | Velocity **3.4.x** |
| `aero-bungee` | `aero-bungee-<version>.jar` | BungeeCord / Waterfall |

Compatibility matrix: [`compat/aero-compat.yml`](compat/aero-compat.yml).

> **Deprecated:** `aero-paper-<version>.jar` → use `aero-paper-1_21-<version>.jar`.

## Requirements

- JDK **21** (builds most modules; toolchain auto-provisions)
- JDK **25** required to compile `aero-paper-26` (Paper 26.x)
- Gradle Wrapper (included)

## Build

```bash
./gradlew build
```

Plugin JARs:

- `aero-bukkit-1_8/build/libs/aero-bukkit-1_8-<version>.jar`
- `aero-bukkit-1_13/build/libs/aero-bukkit-1_13-<version>.jar`
- `aero-paper-1_17/build/libs/aero-paper-1_17-<version>.jar`
- `aero-paper-1_21/build/libs/aero-paper-1_21-<version>.jar`
- `aero-paper-26/build/libs/aero-paper-26-<version>.jar`
- `aero-velocity/build/libs/aero-velocity-<version>.jar`
- `aero-bungee/build/libs/aero-bungee-<version>.jar`

## Install (today)

Manual only:

1. Build or download a release asset matching your MC/proxy version ([COMPATIBILITY.md](docs/COMPATIBILITY.md)).
2. Drop the JAR into the server/proxy `plugins/` folder (or an Aelion Cloud template).
3. Restart the process.

Default config is split into two files:

**`config.yml`** (template-safe — no secrets):

```yaml
config-version: 1
```

**`aero.ae`** (identity — injected by Aelion Cloud, or create manually):

```json
{
  "panelUrl": "https://panel.example.com",
  "serverId": "cms_...",
  "token": "<server-scoped-token>",
  "panelInsecureSsl": false,
  "control": {
    "enabled": true,
    "bind": "127.0.0.1",
    "port": 25580,
    "token": "<control-token>"
  }
}
```

On Velocity/Bungee, set `control.enabled` and `control.token` in `aero.ae` to accept daemon (or curl) backend updates on loopback. For local panels with self-signed TLS, set `panelInsecureSsl: true` (cloud setting `aero.panelInsecureSsl` when injected).

## Commands

| Command | Permission | Notes |
|---------|------------|--------|
| `/ae` / `/aec` | `aelion.aero.info` | Proxy primary + alias (Velocity/Bungee) |
| `/aes` | `aelion.aero.info` | Backend primary (Bukkit/Paper) |
| `/ae help` or `/aes help` | info | Usage (proxy help includes create/backends) |
| `/ae info` or `/aes info` | info | Version, server-id, panel host (no tokens) |
| `/ae reload` or `/aes reload` | `aelion.aero.admin` | Reload config (+ restart control API on Velocity) |
| `/ae ping` or `/aes ping` | info | Hit panel health/info |
| `/ae servers list [--names]` or `/aes …` | info | Panel fleet for same owner |
| `/ae kick <player> [message…]` or `/aes …` | `aelion.aero.admin` | Kick online player |
| `/ae transfer <player> server=\|group=` or `/aes …` | `aelion.aero.admin` | Transfer via fleet / proxy switch |
| `/ae backends` | info | Live proxy backends (Velocity/Bungee only) |
| `/ae create-server …` | `aelion.aero.create` | Proxy only; `template=` XOR `software=`+`version=` |

## Cloud integration

See **[docs/CLOUD_INTEGRATION.md](docs/CLOUD_INTEGRATION.md)** for:

- Panel `/api/aero/v1` routes
- Daemon localhost `PUT` after proxy sync
- Config / token injection
- Matrix-based JAR delivery (cloud#328)

## Delivery (planned, Aelion Cloud)

1. Panel fetches GitHub Release assets (private repo → `AERO_GITHUB_TOKEN` on panel only).
2. Cache under e.g. `data/aero-plugins/<productVer>/`.
3. Select artifact via [`compat/aero-compat.yml`](compat/aero-compat.yml); install into instance `plugins/`.

## Release

Uses **release-please** on `main`: a Release PR is opened for review; after you **approve and merge** it, the tag + GitHub Release are created and CI uploads the six plugin JARs.

See [docs/RELEASE.md](docs/RELEASE.md). Enable branch protection on `main` (require PR + approving review) so that PR is the approval gate.

## License

Proprietary — Aelion Solutions. All rights reserved unless otherwise stated.
