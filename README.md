# Aelion Aero

Paper/Spigot, Velocity, and BungeeCord/Waterfall plugins that connect Minecraft to
[Aelion Cloud](https://github.com/Aelion-Solutions/aelion-cloud).

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
| `/ae` / `/aec` | `aelion.aero.info` (or admin/create) | Proxy primary + alias (Velocity/Bungee); defaults **op** |
| `/aes` | `aelion.aero.info` (or admin/create) | Backend primary (Bukkit/Paper); defaults **op** |
| `/ae help` or `/aes help` | info | Usage (filtered to verbs you can run) |
| `/ae info` or `/aes info` | info | Version, server-id, panel host (no tokens) |
| `/ae reload` or `/aes reload` | `aelion.aero.admin` | Reload config (+ restart control API on Velocity) |
| `/ae ping` or `/aes ping` | info | Hit panel health/info |
| `/ae servers list [--names]` or `/aes …` | info | Panel fleet for same owner |
| `/ae notify [on\|off]` | info | Proxy only — toggle fleet change chat (session; daemon push) |
| `/ae kick <player> [message…]` or `/aes …` | `aelion.aero.admin` | Kick online player |
| `/ae transfer <player> server=\|group=` or `/aes …` | `aelion.aero.admin` | Transfer via fleet / proxy switch |
| `/ae backends` | info | Live proxy backends (Velocity/Bungee only) |
| `/ae create-server …` | `aelion.aero.create` | Proxy only; `template=` XOR `software=`+`version=` |

`admin` and `create` each imply `info` (YAML children + code). They do not imply each other.

## Cloud integration

See **[docs/CLOUD_INTEGRATION.md](docs/CLOUD_INTEGRATION.md)** for:

- Panel `/api/aero/v1` routes
- Daemon localhost `PUT` after proxy sync
- Config / token injection
- Matrix-based JAR delivery (cloud#328)

## Release

Uses **release-please** on `main`: a Release PR is opened for review; after you **approve and merge** it, the tag + GitHub Release are created and CI uploads the six plugin JARs.

See [docs/RELEASE.md](docs/RELEASE.md). Enable branch protection on `main` (require PR + approving review) so that PR is the approval gate.

## License

Proprietary — Aelion Solutions. All rights reserved unless otherwise stated.
