# Localhost control API

Implemented in Aero. Used by the Aelion Cloud daemon for **proxy live registry** sync and
**graceful process shutdown**. Loopback-bound; not internet-reachable.

- Bind: loopback only
- Header: `X-Aero-Control-Token: <control.token>` (see [Configuration wiki page](https://github.com/Aelion-Solutions/aelion-aero/wiki/Configuration))
- Enable via `control.enabled` in `aero.ae`

## Live registry (proxies)

On-disk proxy server maps stay empty — Aero mutates the proxy's in-memory server map only. Join
routing uses platform events (lobby → try → any). On deregister, players move to a remaining
lobby/try, or disconnect if none. A proxy restart clears the memory map until the panel/daemon
re-`PUT`s the current registry.

## Endpoints

| Method | Path | Purpose | Platforms |
|--------|------|---------|-----------|
| `GET` | `/v1/health` | `{ "ok": true, "plugin": "AelionAero", "version": "..." }` | all |
| `GET` | `/v1/backends` | Last applied registry | proxies |
| `PUT` | `/v1/backends` | Full replace: `{ "backends": [ { "name", "address", "role" } ] }` | proxies |
| `POST` | `/v1/shutdown` | Drain players + schedule shutdown → `202 { "ok": true }` | Paper + proxies |
| `GET` | `/v1/players` | `{ "players": [ { "uuid", "name" } ] }`, empty → `[]` | Paper + proxies |
| `POST` | `/v1/players/kick` | `{ "uuid", "message"? }` → `{ "ok": true }` / 404 offline | Paper + proxies |
| `POST` | `/v1/players/transfer` | `{ "uuid", "proxyServerName"? \| "serverId"? \| "serverName"? \| "groupId"? \| "groupName"? }` | Paper + proxies |
| `POST` | `/v1/fleet-notify` | `{ "events": [ { "id", "name", "status", "groupId"?, "groupName"? } ] }` → `{ "ok": true, "delivered": N }` | proxies |

Panel operators can also kick/transfer via `POST /api/servers/:id/aero/players/kick|transfer`
(daemon forwards to the same localhost paths). Use the `:id` of the server where the player is online.

## Manual test (no cloud)

```bash
# config: control.enabled=true, control.token=devsecret
curl -s -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/health

curl -s -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/players

curl -s -X PUT http://127.0.0.1:25580/v1/backends \
  -H "X-Aero-Control-Token: devsecret" \
  -H "Content-Type: application/json" \
  -d '{"backends":[{"name":"lobby","address":"127.0.0.1:25565","role":"lobby"}]}'

curl -s -X POST -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/shutdown
```

## Daemon usage (aelion-cloud)

Daemon reads control port/token from `.aelion-aero.ae` and:

- `PUT`s backends after proxy sync, when the process is running
- `GET`s `/v1/players` for online uuid/name lists (Paper + proxies)
- `POST`s `/v1/players/kick` and `/v1/players/transfer` for panel operator actions
- `POST`s `/v1/fleet-notify` to same-owner running proxies on server status/create/delete
- On graceful stop: `POST /v1/shutdown`, brief wait, then stdin `stop`/`shutdown`/`end`, then force-kill

See cloud `daemon/internal/aero/` and `docs/AELION_AERO.md` in **aelion-cloud**.
