# Aelion Cloud ↔ Aero integration contract

Contract between **aelion-aero** (plugins) and **aelion-cloud** (panel + daemon).
Keep this file aligned with cloud `docs/AELION_AERO.md` when behavior changes.

## Architecture

| Concern | Path | Auth | Status |
|---------|------|------|--------|
| Health / server info | Plugin → panel HTTPS `/api/aero/v1` | `Authorization: Bearer <server-token>` | **Live in cloud** |
| Fleet list / create (proxy) | Plugin → panel HTTPS | Same Bearer; create attaches proxy child | **Live in cloud** |
| Hot backend sync | Panel → daemon RPC → localhost Aero | `X-Aero-Control-Token` + bind `127.0.0.1` | **Live** (Velocity + Bungee) |
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

Write into:

| Software | Plugin `config.yml` path |
|----------|--------------------------|
| Paper / Spigot | `plugins/AelionAero/config.yml` |
| Velocity | `plugins/aelionaero/config.yml` |
| BungeeCord / Waterfall | `plugins/AelionAero/config.yml` |

Plus daemon sidecar `.aelion-aero.ae` (JSON) for hot-reload notify.

```yaml
panel-url: "https://panel.example.com"
server-id: "cms_..."
token: "<server-scoped-token>"
control:
  enabled: true          # true for Velocity/Bungee; false for Paper
  bind: "127.0.0.1"
  port: 25580            # cloud should allocate unique ports per proxy (follow-up)
  token: "<control-token>"
```

- **Server token**: first-party credential scoped to one server id (not a user API key).
- **Control token**: known to daemon + proxy Aero only; never expose to players or panel public APIs.

## Panel routes

Prefix: `/api/aero/v1` (Aero contract envelope — not the standard panel `{ success, data }` shape).

| Method | Path | Purpose | Status |
|--------|------|---------|--------|
| `GET` | `/api/aero/v1/health` | `/ae ping` | Live |
| `GET` | `/api/aero/v1/servers` | Same-owner fleet (+ players, group, joinable, proxyName) | Live in cloud |
| `GET` | `/api/aero/v1/servers/:id` | ping fallback (token must match `:id`) | Live |
| `GET` | `/api/aero/v1/groups` | Same-owner groups + member snapshots (signs / NPCs) | Live in cloud |
| `POST` | `/api/aero/v1/servers` | Proxy-only create: `template` XOR `software`+`version`; attaches to actor proxy | Live in cloud |
| `POST` | `/api/aero/v1/groups` | body = Aero `CreateGroupRequest` | Live |

Java client: `com.aelion.aero.common.api.HttpPanelClient`.

### Fleet bridge for sibling plugins

Every **backend** Aero band registers `com.aelion.aero.api.AeroFleetService` on Bukkit
ServicesManager (`aero-bukkit-1_8`, `aero-bukkit-1_13`, `aero-paper-1_17`, `aero-paper-1_21`).
First-party plugins (Signs, later NPCs) depend on `AelionAero` and look up that service — they do
**not** carry their own panel tokens.

- `listServers()` / `listGroups()` — cached panel poll (~2s TTL)
- `connectPlayer(uuid, proxyServerName)` — BungeeCord plugin messaging `Connect` (Velocity legacy channel)

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

## Proxy control API (implemented in Aero)

**Live registry:** on-disk proxy server maps stay empty. Aero mutates the proxy’s
in-memory server map only. Join routing uses platform events (lobby → try → any).
On deregister, players are moved to a remaining lobby/try, or disconnected if none.

- `GET /v1/health` — `{ "ok": true, "plugin": "AelionAero", "version": "..." }`
- `GET /v1/backends` — last applied registry
- `PUT /v1/backends` — full replace `{ "backends": [ { "name", "address", "role" } ] }`
- Header: `X-Aero-Control-Token: <control.token>`
- Bind: loopback only

A proxy restart clears the memory map until the panel/daemon re-PUTs the current registry.

### Manual test (no cloud)

```bash
# config: control.enabled=true, control.token=devsecret
curl -s -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/health

curl -s -X PUT http://127.0.0.1:25580/v1/backends \
  -H "X-Aero-Control-Token: devsecret" \
  -H "Content-Type: application/json" \
  -d '{"backends":[{"name":"lobby","address":"127.0.0.1:25565","role":"lobby"}]}'
```

## Daemon notify (live in aelion-cloud)

Cloud daemon reads control port/token from `.aelion-aero.ae` and `PUT`s backends
to `http://127.0.0.1:<port>/v1/backends` after proxy sync when the process is running.
See cloud `daemon/internal/aero/` and `docs/AELION_AERO.md`.

## Plugin JAR delivery (cloud follow-up)

1. Panel fetches GitHub Release assets from `aelion-aero` (private → `AERO_GITHUB_TOKEN` on **panel only**).
2. Cache under e.g. `data/aero-plugins/<productVer>/`.
3. Select the artifact with [`compat/aero-compat.yml`](../compat/aero-compat.yml) (see [COMPATIBILITY.md](COMPATIBILITY.md)):
   - Map instance `software` + MC/proxy version → unique matrix row
   - Install `<artifact>-<productVer>.jar` into instance `plugins/`
   - No match → fail provision (do not guess)
4. Backend bands: `aero-bukkit-1_8`, `aero-bukkit-1_13`, `aero-paper-1_17`, `aero-paper-1_21`
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
| `aelion.aero.admin` | op | reload |
| `aelion.aero.create` | op | `/ae create-server` (proxy only) |
