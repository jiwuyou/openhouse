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
group:smallphone-apps
```

## Current Managed Services

| Service | Group | Current Ports | Target Ports |
| --- | --- | --- | --- |
| `cc-connect` | `local-stack` | `9810`, `9820`, `9840` | `21010`, `21020`, `21040` |
| `smallphone-stack` | `local-stack` | `3100`, `18080`, `18082`, `18096` | `22000`, `22080`, `22082`, `22096` |
| `file-transfer-18081` | `local-stack` | `18081` | `22081` |
| `smallphone-like-girl` | `smallphone-apps` | `4103` | `23003` |
| `smallphone-like-girl-clone` | `smallphone-apps` | `4108` | `23008` |

## SmallPhone App Tags

SmallPhone App Management maps bundled standalone apps to service-manager
records with service tags. Keep these tags stable when ports migrate:

| App | Service | Required Tags |
| --- | --- | --- |
| LikeGirl | `smallphone-like-girl` | `smallphone-app:like-girl`, `smallphone-instance:like-girl`, `group:smallphone-apps` |
| LikeGirl clone | `smallphone-like-girl-clone` | `smallphone-app:like-girl-clone`, `smallphone-instance:like-girl-clone`, `group:smallphone-apps` |

The SmallPhone frontend should call SmallPhone Core proxy endpoints under
`/api/service-manager/*`; browser code should not call service-manager directly
or carry the service-manager bearer token.

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
