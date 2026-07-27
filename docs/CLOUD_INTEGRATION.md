# Aelion Cloud ↔ Aero integration contract

Contract between **aelion-aero** (plugins) and **aelion-cloud** (panel + daemon).
Keep this file aligned with cloud `docs/AELION_AERO.md` when behavior changes.

## Architecture

| Concern | Path | Auth | Status |
|---------|------|------|--------|
| Health / server info | Plugin → panel HTTPS `/api/aero/v1` | `Authorization: Bearer <server-token>` | **Live in cloud** |
| Fleet list / create (proxy) | Plugin → panel HTTPS | Same Bearer; create attaches proxy child | **Live in cloud** |
| Hot backend sync | Panel → daemon RPC → localhost Aero | `X-Aero-Control-Token` + bind `127.0.0.1` | **Live** (Velocity + Bungee) |
| Graceful stop | Daemon → localhost Aero `POST /v1/shutdown` → stdin stop → kill | `X-Aero-Control-Token` | **Live** (Paper + proxies) |
| On-disk proxy config | Daemon keeps server maps empty | Live map is Aero memory only | **Live** |

```text
scale member RUNNING
  → panel syncProxyBackends
  → daemon keeps on-disk proxy server maps empty (no live names written)
  → daemon PUT http://127.0.0.1:<control.port>/v1/backends
  → Aero registers/unregisters servers in process memory
  → Aero routes new logins (Velocity PlayerChooseInitialServerEvent / Bungee ServerConnectEvent)
  → on deregister: move players to a remaining lobby/try, else disconnect
```

## Config injection (provision / start)

Cloud injects **identity only** — never overwrites operator `config.yml` (safe in templates).

| File | Owner | Path |
|------|--------|------|
| Identity JSON | Cloud | `plugins/AelionAero/aero.ae` (Velocity: `plugins/aelionaero/aero.ae`) |
| Operator YAML | Template / operator | same folder `config.yml` |
| Daemon sidecar | Cloud | `.aelion-aero.ae` (server root; same JSON as identity) |

Example `aero.ae` (injected):

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

Example operator `config.yml` (template-safe):

```yaml
config-version: 1
```

- **Server token**: first-party credential scoped to one server id (not a user API key).
- **Control token**: known to daemon + Aero only; never expose to players or panel public APIs.
- **`panelInsecureSsl`**: from panel setting `aero.panelInsecureSsl` (local/self-signed panels). Never enable in production.
- Plugin merges `aero.ae` over `config.yml` on load/`/ae reload`. Legacy identity keys still in `config.yml` work until the next inject/migration.

## Panel routes

Prefix: `/api/aero/v1` (Aero contract envelope — not the standard panel `{ success, data }` shape).

| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| `GET` | `/api/aero/v1/health` | `/ae ping` / `/aes ping` | Live |
| `GET` | `/api/aero/v1/servers` | Same-owner fleet (+ players, group, joinable, proxyName) | Live in cloud |
| `GET` | `/api/aero/v1/servers/:id` | ping fallback (token must match `:id`) | Live |
| `GET` | `/api/aero/v1/groups` | Same-owner groups + member snapshots (signs / NPCs) | Live in cloud |
| `POST` | `/api/aero/v1/servers` | Proxy-only create: `template` XOR `software`+`version`; attaches to actor proxy | Live in cloud |
| `POST` | `/api/aero/v1/groups` | body = Aero `CreateGroupRequest` | Live |

Java client: `com.aelion.aero.common.api.HttpPanelClient`.

### Fleet bridge for sibling plugins

Every **backend** Aero band registers `com.aelion.aero.api.AeroFleetService` on Bukkit
ServicesManager (`aero-bukkit-1_8`, `aero-bukkit-1_13`, `aero-paper-1_17`, `aero-paper-1_21`,
`aero-paper-26`).
First-party plugins (Signs, later NPCs) depend on `AelionAero` and look up that service — they do
**not** carry their own panel tokens.

- `listServers()` / `listGroups()` — cached panel poll (~2s TTL)
- `connectPlayer(uuid, proxyServerName)` — BungeeCord plugin messaging `Connect` (Velocity legacy channel)
- `kickPlayer(uuid, message)` — local kick (blank message → default reason)
- `transferToServer(uuid, idOrName)` / `transferToGroup(uuid, idOrName)` — resolve fleet then Connect

In-game: `/aes|ae kick <player> [message…]` and
`/aes|ae transfer <player> server=<id|name>|group=<id|name>` (`aelion.aero.admin`).
Proxies kick/transfer natively; backends use the fleet bridge.

#### Maven (`aero-api`)

Thin compile-only artifact (no Minecraft deps), published to GitHub Packages on each Aero release
(and via **Actions → Publish aero-api**):

```text
com.aelion.aero:aero-api:<aero-version>
https://maven.pkg.github.com/Aelion-Solutions/aelion-aero
```

Consumers need a GitHub token with `read:packages` (and SSO authorized for the org if required):

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/Aelion-Solutions/aelion-aero")
    credentials {
        username = System.getenv("GITHUB_ACTOR") ?: "token"
        password = System.getenv("GITHUB_TOKEN") ?: ""
    }
}
dependencies {
    compileOnly("com.aelion.aero:aero-api:0.2.0")
}
```

At runtime, types come from the Aero plugin JAR (do not shade `aero-api` into sibling plugins).

### Create-server body (proxy Aero token)

- Required: `name`
- XOR: `template` (template **name**) **or** both `software` and `version`
- Defaults: `autoStart=true` when omitted; attach role `backend` (optional `role`: `backend`\|`lobby`\|`try`)
- Actor must be proxy software; new server is attached as a proxy child then backends sync

## Localhost control API (implemented in Aero)

Used by the daemon for **proxy live registry** and **graceful process shutdown**.

**Live registry (proxies):** on-disk proxy server maps stay empty. Aero mutates the proxy’s
in-memory server map only. Join routing uses platform events (lobby → try → any).
On deregister, players are moved to a remaining lobby/try, or disconnected if none.

- `GET /v1/health` — `{ "ok": true, "plugin": "AelionAero", "version": "..." }`
- `GET /v1/backends` — last applied registry (proxies only)
- `PUT /v1/backends` — full replace `{ "backends": [ { "name", "address", "role" } ] }` (proxies only)
- `POST /v1/shutdown` — drain players + schedule platform shutdown; returns `202 { "ok": true }` (Paper + proxies)
- `POST /v1/players/kick` — `{ "uuid", "message"? }` → `{ "ok": true }` / 404 offline (Paper + proxies)
- `POST /v1/players/transfer` — `{ "uuid", "proxyServerName"? | "serverId"? | "serverName"? | "groupId"? | "groupName"? }` (Paper + proxies)
- Header: `X-Aero-Control-Token: <control.token>`
- Bind: loopback only

A proxy restart clears the memory map until the panel/daemon re-PUTs the current registry.

Panel operators can also kick/transfer via
`POST /api/servers/:id/aero/players/kick|transfer` (daemon → same localhost paths).
Call `:id` where the player is online.

### Manual test (no cloud)

```bash
# config: control.enabled=true, control.token=devsecret
curl -s -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/health

curl -s -X PUT http://127.0.0.1:25580/v1/backends \
  -H "X-Aero-Control-Token: devsecret" \
  -H "Content-Type: application/json" \
  -d '{"backends":[{"name":"lobby","address":"127.0.0.1:25565","role":"lobby"}]}'

curl -s -X POST -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/shutdown
```

## Daemon notify (live in aelion-cloud)

Cloud daemon reads control port/token from `.aelion-aero.ae` and:

- `PUT`s backends to `http://127.0.0.1:<port>/v1/backends` after proxy sync when the process is running
- `POST`s `/v1/players/kick` and `/v1/players/transfer` for panel operator actions
- On graceful stop: `POST /v1/shutdown`, brief wait, then stdin `stop`/`shutdown`/`end`, then force-kill

See cloud `daemon/internal/aero/` and `docs/AELION_AERO.md`.

## Plugin JAR delivery (cloud follow-up)

1. Panel fetches GitHub Release assets from `aelion-aero` (private → `AERO_GITHUB_TOKEN` on **panel only**).
2. Cache under e.g. `data/aero-plugins/aero/<productVer>/` (JARs + `aero-compat.yml`).
3. Load that release’s [`compat/aero-compat.yml`](../compat/aero-compat.yml) (release asset, or Contents API at the tag for older releases) — see [COMPATIBILITY.md](COMPATIBILITY.md):
   - Map instance `software` + MC/proxy version → unique matrix row
   - Install `<artifact>-<productVer>.jar` into instance `plugins/`
   - No match → fail provision (do not guess)
   - Product versions below cloud `minProductVersion` (`0.6.0`, first `aero.ae` release) are rejected (`AERO_VERSION_TOO_OLD`)
4. Backend bands: `aero-bukkit-1_8`, `aero-bukkit-1_13`, `aero-paper-1_17`, `aero-paper-1_21`, `aero-paper-26`
5. Proxies: `aero-velocity`, `aero-bungee`

**Note:** `aero-paper-<ver>.jar` is deprecated; use `aero-paper-1_21-<ver>.jar`.

Tracked as [aelion-cloud#328](https://github.com/Aelion-Solutions/aelion-cloud/issues/328).

## REST sync / TypeSpec (later)

- Source of truth for Aero REST: TypeSpec in **aelion-cloud** → OpenAPI `aero-v1.json`.
- Aero regenerates `com.aelion.aero.common.api` via openapi-generator and **commits** generated sources.
- Until then, hand-written DTOs in that package are a bootstrap — do not invent a second dialect.

## Permissions

| Node | Default | Use |
|------|---------|-----|
| `aelion.aero.info` | true | help/info/ping/servers list/backends |
| `aelion.aero.admin` | op | reload, kick, transfer |
| `aelion.aero.create` | op | `/ae create-server` (proxy only) |
