# Aelion Cloud ↔ Aero integration contract

Contract between **aelion-aero** (plugins) and **aelion-cloud** (panel + daemon).
Keep this file aligned with cloud `docs/AELION_AERO.md` when behavior changes.

## Architecture

| Concern | Path | Auth | Status |
|---------|------|------|--------|
| Health / server info | Plugin → panel HTTPS `/api/aero/v1` | Bearer server token | Live in cloud |
| Fleet list / create (proxy) | Plugin → panel HTTPS | Same Bearer; create attaches proxy child | Live in cloud |
| Hot backend sync | Panel → daemon RPC → localhost Aero | Control token, loopback | Live (Velocity + Bungee) |
| Graceful stop | Daemon → localhost Aero `POST /v1/shutdown` → stdin stop → kill | Control token | Live (Paper + proxies) |
| On-disk proxy config | Daemon keeps server maps empty | Live map is Aero memory only | Live |

```text
scale member RUNNING
  → panel syncProxyBackends
  → daemon keeps on-disk proxy server maps empty (no live names written)
  → daemon PUT http://127.0.0.1:<control.port>/v1/backends
  → Aero registers/unregisters servers in process memory
  → Aero routes new logins (Velocity PlayerChooseInitialServerEvent / Bungee ServerConnectEvent)
  → on deregister: move players to a remaining lobby/try, else disconnect
```

Localhost control API (health, backends, shutdown, kick/transfer, fleet-notify): see
[CONTROL_API.md](CONTROL_API.md).

## Config injection

Cloud injects **identity only** — never overwrites operator `config.yml`. Field reference and
example: [Configuration wiki page](https://github.com/Aelion-Solutions/aelion-aero/wiki/Configuration).

| File | Owner | Path |
|------|-------|------|
| Identity JSON | Cloud | `plugins/AelionAero/aero.ae` (Velocity: `plugins/aelionaero/aero.ae`) |
| Operator YAML | Template / operator | same folder `config.yml` |
| Daemon sidecar | Cloud | `.aelion-aero.ae` (server root; same JSON as identity) |

## Panel routes

Prefix: `/api/aero/v1` (Aero contract envelope — not the standard panel `{ success, data }` shape).

| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| `GET` | `/api/aero/v1/health` | `/ae ping` / `/aes ping` | Live |
| `GET` | `/api/aero/v1/servers` | Same-owner fleet (+ players, group, joinable, proxyName) | Live in cloud |
| `GET` | `/api/aero/v1/servers/:id` | Ping fallback (token must match `:id`) | Live |
| `GET` | `/api/aero/v1/groups` | Same-owner groups + member snapshots (signs / NPCs) | Live in cloud |
| `POST` | `/api/aero/v1/servers` | Proxy-only create: `template` XOR `software`+`version`; attaches to actor proxy | Live in cloud |
| `POST` | `/api/aero/v1/groups` | Body = Aero `CreateGroupRequest` | Live |

Java client: `com.aelion.aero.common.api.HttpPanelClient`.

### Fleet bridge for sibling plugins

Every **backend** Aero band registers `com.aelion.aero.api.AeroFleetService` on Bukkit
ServicesManager (`aero-bukkit-1_8`, `aero-bukkit-1_13`, `aero-paper-1_17`, `aero-paper-1_21`,
`aero-paper-26`). First-party plugins (Signs, later NPCs) depend on `AelionAero` and look up that
service instead of carrying their own panel tokens.

- `listServers()` / `listGroups()` — cached panel poll (~2s TTL)
- `connectPlayer(uuid, proxyServerName)` — BungeeCord plugin messaging `Connect` (Velocity legacy channel)
- `kickPlayer(uuid, message)` — local kick (blank message → default reason)
- `transferToServer(uuid, idOrName)` / `transferToGroup(uuid, idOrName)` — resolve fleet then `Connect`

Command surface: [Commands wiki page](https://github.com/Aelion-Solutions/aelion-aero/wiki/Commands).

#### Maven (`aero-api`)

Thin compile-only artifact (no Minecraft deps), published to `maven.aelion.solutions` on each Aero
release (and via **Actions → Publish aero-api**). Public, no auth needed to consume:

```text
com.aelion.aero:aero-api:<aero-version>
https://maven.aelion.solutions/releases
```

```kotlin
repositories {
    maven { url = uri("https://maven.aelion.solutions/releases") }
}
dependencies {
    compileOnly("com.aelion.aero:aero-api:0.2.0")
}
```

Runtime types come from the Aero plugin JAR — do not shade `aero-api` into sibling plugins.

### Create-server body (proxy Aero token)

- Required: `name`
- XOR: `template` (template **name**) or both `software` and `version`
- Defaults: `autoStart=true` when omitted; attach role `backend` (optional `role`: `backend`\|`lobby`\|`try`)
- Actor must be proxy software; new server attaches as a proxy child, then backends sync

## Plugin JAR delivery (cloud follow-up)

1. Panel fetches GitHub Release assets from `aelion-aero` (private → `AERO_GITHUB_TOKEN` on **panel only**).
2. Cache under e.g. `data/aero-plugins/aero/<productVer>/` (JARs + `aero-compat.yml`).
3. Load that release's [`compat/aero-compat.yml`](../compat/aero-compat.yml) — see [COMPATIBILITY.md](COMPATIBILITY.md):
   - Map instance `software` + MC/proxy version → unique matrix row
   - Install `<artifact>-<productVer>.jar` into instance `plugins/`
   - No match → fail provision (do not guess)
   - Product versions below cloud `minProductVersion` (`0.6.0`, first `aero.ae` release) are rejected (`AERO_VERSION_TOO_OLD`)

Backend bands: `aero-bukkit-1_8`, `aero-bukkit-1_13`, `aero-paper-1_17`, `aero-paper-1_21`, `aero-paper-26`.
Proxies: `aero-velocity`, `aero-bungee`.

**Note:** `aero-paper-<ver>.jar` is deprecated; use `aero-paper-1_21-<ver>.jar`.

Tracked as [aelion-cloud#328](https://github.com/Aelion-Solutions/aelion-cloud/issues/328).

## REST sync / TypeSpec (later)

- Source of truth for Aero REST: TypeSpec in **aelion-cloud** → OpenAPI `aero-v1.json`.
- Aero regenerates `com.aelion.aero.common.api` via openapi-generator and **commits** generated sources.
- Until then, hand-written DTOs in that package are a bootstrap — do not invent a second dialect.
