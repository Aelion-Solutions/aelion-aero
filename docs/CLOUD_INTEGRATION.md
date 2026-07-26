# Aelion Cloud ↔ Aero integration contract

This document describes what **aelion-cloud** must add later. Aero implements the plugin side now; do not treat these panel routes as live until cloud ships them.

## Architecture

| Concern | Path | Auth |
|---------|------|------|
| Admin / create / info | Plugin → panel HTTPS | `Authorization: Bearer <server-token>` from `config.yml` |
| Hot backend sync | Panel → daemon RPC → localhost Aero Velocity | `X-Aero-Control-Token` + bind `127.0.0.1` only |
| On-disk `velocity.toml` | Daemon `UpdateProxyBackends` (already exists) | Unchanged |

```text
scale member RUNNING
  → panel syncProxyBackends
  → daemon ApplyBackendRegistry (files)
  → daemon PUT http://127.0.0.1:<control.port>/v1/backends
  → Aero registers servers live
  → on success: clear proxyConfigPendingRestart
  → on failure: keep restart-pending (today’s behavior)
```

## Config injection (provision / start)

Write into Paper `plugins/AelionAero/config.yml` or Velocity plugin data `config.yml`:

```yaml
panel-url: "https://panel.example.com"
server-id: "cms_..."
token: "<server-scoped-token>"
control:
  enabled: true          # Velocity proxies only
  bind: "127.0.0.1"
  port: 25580
  token: "<control-token>"
```

- **Server token**: first-party credential scoped to one server id (not a user API key).
- **Control token**: known to daemon + Velocity Aero only; never expose to players or panel public APIs.

## Panel routes to add

Prefix: `/api/aero/v1` (or proxy to existing `/api/servers` / `/api/groups` with Aero auth).

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/aero/v1/health` | `/ae ping` |
| `GET` | `/api/aero/v1/servers/:id` | `/ae info` / ping fallback |
| `POST` | `/api/aero/v1/servers` | body = Aero `CreateServerRequest` |
| `POST` | `/api/aero/v1/groups` | body = Aero `CreateGroupRequest` |

Java client already posts to these paths (`com.aelion.aero.common.api.HttpPanelClient`).

### Example: Java create server (plugin later)

```java
CreateServerRequest req = new CreateServerRequest();
req.setName("dev-1");
req.setType("survival");
req.setSoftware("paper");
req.setMemory(2048);

CreateServerResponse created = new HttpPanelClient(config).createServer(req);
```

## Velocity control API (implemented in Aero)

- `GET /v1/health` — `{ "ok": true, "plugin": "AelionAero", "version": "..." }`
- `GET /v1/backends` — last applied registry
- `PUT /v1/backends` — full replace `{ "backends": [ { "name", "address", "role" } ] }`
- Header: `X-Aero-Control-Token: <control.token>`
- Bind: loopback only

### Manual test (no cloud)

```bash
# config: control.enabled=true, control.token=devsecret
curl -s -H "X-Aero-Control-Token: devsecret" http://127.0.0.1:25580/v1/health

curl -s -X PUT http://127.0.0.1:25580/v1/backends \
  -H "X-Aero-Control-Token: devsecret" \
  -H "Content-Type: application/json" \
  -d '{"backends":[{"name":"lobby","address":"127.0.0.1:25565","role":"lobby"}]}'
```

## Daemon snippet (add later in aelion-cloud)

After successful `ApplyBackendRegistry` in `dispatch_proxy.go`, notify Aero when the proxy process is running:

```go
func notifyAeroBackends(controlPort int, controlToken string, body []byte) error {
    url := fmt.Sprintf("http://127.0.0.1:%d/v1/backends", controlPort)
    req, err := http.NewRequest(http.MethodPut, url, bytes.NewReader(body))
    if err != nil {
        return err
    }
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("X-Aero-Control-Token", controlToken)
    client := &http.Client{Timeout: 3 * time.Second}
    resp, err := client.Do(req)
    if err != nil {
        return err // fall back: proxyConfigPendingRestart
    }
    defer resp.Body.Close()
    if resp.StatusCode < 200 || resp.StatusCode >= 300 {
        return fmt.Errorf("aero control HTTP %d", resp.StatusCode)
    }
    return nil
}
```

Read `control.port` / `control.token` from the proxy’s Aero `config.yml` (or mirror them in daemon metadata at provision time).

On success, panel should clear `proxyConfigPendingRestart`. On failure / connection refused, keep today’s restart banner.

## Plugin JAR delivery (later)

1. Panel fetches GitHub Release assets from `aelion-aero` (private → `AERO_GITHUB_TOKEN` / PAT on **panel only**).
2. Cache under e.g. `backend/data/aero-plugins/<version>/`.
3. Distribute to daemons (template-like sync) into `plugins/`.

## REST sync / TypeSpec (later)

- Source of truth for Aero REST: TypeSpec in **aelion-cloud** → OpenAPI `aero-v1.json`.
- Aero regenerates `com.aelion.aero.common.api` via openapi-generator and **commits** generated sources.
- Until then, hand-written DTOs in that package are a bootstrap — do not invent a second dialect.

## Permissions

| Node | Default | Use |
|------|---------|-----|
| `aelion.aero.info` | true | help/info/ping |
| `aelion.aero.admin` | op | reload |
| `aelion.aero.create` | op | reserved for future `/ae create-*` |
