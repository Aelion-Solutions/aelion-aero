# Aelion Aero

Paper, Velocity, and BungeeCord/Waterfall plugins that connect Minecraft to
[Aelion Cloud](https://github.com/Aelion-Solutions/aelion-cloud).

Related: [aelion-cloud#308](https://github.com/Aelion-Solutions/aelion-cloud/issues/308)

## Status (phase 2)

- `/ae` and `/aec`: `help`, `info`, `reload`, `ping`, `servers list` (Paper uses Brigadier lifecycle registration so bare `/ae` works; styled `[Aero]` prefix + colors)
- Proxy-only: `backends`, `create-server` (template XOR software+version; panel attaches to calling proxy)
- Shared panel client + create DTOs in `aero-common`
- Proxy **live backend registry**: empty on-disk server maps; process memory via `PUT /v1/backends`
- Live join routing (Velocity `PlayerChooseInitialServerEvent` / Bungee `ServerConnectEvent`)
- On backend deregister: move players to a lobby/try target, or disconnect if none
- Cloud wiring documented in [docs/CLOUD_INTEGRATION.md](docs/CLOUD_INTEGRATION.md)

## Modules

| Module | Artifact | Platform |
|--------|----------|----------|
| `aero-common` | (library) | Config, control DTOs, panel client, create models |
| `aero-paper` | `aero-paper-<version>.jar` | Paper 1.21.x |
| `aero-velocity` | `aero-velocity-<version>.jar` | Velocity 3.4.x |
| `aero-bungee` | `aero-bungee-<version>.jar` | BungeeCord / Waterfall |

## Requirements

- JDK **21**
- Gradle Wrapper (included)

## Build

```bash
./gradlew build
```

Plugin JARs:

- `aero-paper/build/libs/aero-paper-<version>.jar`
- `aero-velocity/build/libs/aero-velocity-<version>.jar`
- `aero-bungee/build/libs/aero-bungee-<version>.jar`

## Install (today)

Manual only:

1. Build or download a release asset.
2. Drop the JAR into the server/proxy `plugins/` folder (or an Aelion Cloud template).
3. Restart the process.

Default config:

```yaml
panel-url: ""
server-id: ""
token: ""
control:
  enabled: false
  bind: "127.0.0.1"
  port: 25580
  token: ""
```

On Velocity, set `control.enabled: true` and a `control.token` to accept daemon (or curl) backend updates on loopback.

## Commands

| Command | Permission | Notes |
|---------|------------|--------|
| `/ae` / `/aec` | `aelion.aero.info` | Primary + alias |
| `/ae help` | info | Usage (proxy help includes create/backends) |
| `/ae info` | info | Version, server-id, panel host (no tokens) |
| `/ae reload` | `aelion.aero.admin` | Reload config (+ restart control API on Velocity) |
| `/ae ping` | info | Hit panel health/info |
| `/ae servers list [--names]` | info | Panel fleet for same owner |
| `/ae backends` | info | Live proxy backends (Velocity/Bungee only) |
| `/ae create-server …` | `aelion.aero.create` | Proxy only; `template=` XOR `software=`+`version=` |

## Cloud integration

See **[docs/CLOUD_INTEGRATION.md](docs/CLOUD_INTEGRATION.md)** for:

- Panel `/api/aero/v1` routes to add
- Daemon localhost `PUT` after `UpdateProxyBackends`
- Config / token injection
- Future TypeSpec → OpenAPI → Java codegen sync

## Delivery (planned, Aelion Cloud)

1. Panel fetches GitHub Release assets (private repo → `AERO_GITHUB_TOKEN` on panel only).
2. Cache under e.g. `data/aero-plugins/`.
3. Distribute to daemons into instance `plugins/`.

## Release

Uses **release-please** on `main`: a Release PR is opened for review; after you **approve and merge** it, the tag + GitHub Release are created and CI uploads the JARs.

See [docs/RELEASE.md](docs/RELEASE.md). Enable branch protection on `main` (require PR + approving review) so that PR is the approval gate.

## License

Proprietary — Aelion Solutions. All rights reserved unless otherwise stated.
