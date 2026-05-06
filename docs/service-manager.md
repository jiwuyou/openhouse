# Service Manager

`service-manager` is the OpenHouse local runtime control plane.

Repository:

```text
/root/projects/service-manager
```

Web UI:

```text
http://127.0.0.1:8787/
```

## Current Group Convention

Use service tags to define groups:

```text
group:<name>
```

Current group:

```text
group:local-stack
```

## Current Managed Services

| Service | Group | Ports |
| --- | --- | --- |
| `cc-connect` | `local-stack` | `9810`, `9820`, `9840` |
| `smallphone-stack` | `local-stack` | `3100`, `18080`, `18082`, `18096` |
| `file-transfer-18081` | `local-stack` | `18081` |

## Group API

```text
GET  /api/v1/groups
POST /api/v1/groups/:name/start
POST /api/v1/groups/:name/stop
POST /api/v1/groups/:name/restart
```

## Desktop Launcher

Desktop file:

```text
/root/Desktop/Service-Manager.desktop
```

Launcher script:

```text
/root/projects/service-manager/scripts/open-service-manager.sh
```

