# Port Map

This document tracks local OpenHouse ports. Prefer `127.0.0.1` bindings unless
shared-device access requires a LAN or Tailscale address.

## Target Port Namespace

OpenHouse should converge on five-digit `2xxxx` local service ports. This keeps
long-running listeners below the common Linux ephemeral range while leaving room
for per-project blocks.

| Range | Purpose |
| --- | --- |
| `20000-20999` | OpenHouse system control plane |
| `21000-21999` | Runtime bridges, agent bridges, and proxies |
| `22000-22999` | SmallPhone core, shell, and frontend services |
| `23000-24999` | SmallPhone standalone apps |
| `25000-25999` | Development servers and previews |
| `29000-29999` | Experimental or reserved services |

## Current Local Stack

| Port | Service | Repository | Notes |
| --- | --- | --- | --- |
| 8787 | service-manager | `/root/projects/service-manager` | Web UI and API |
| 9810 | cc-connect | `/root/cc-connect-fresh` | Bridge/runtime port |
| 9820 | cc-connect | `/root/cc-connect-fresh` | Management port |
| 9840 | cc-connect | `/root/cc-connect-fresh` | Web client/runtime facade |
| 3100 | smallphone-stack | `/root/projects/smallphone/smallphone-active` | SmallPhone backend |
| 18080 | smallphone-stack | `/root/projects/smallphone/smallphone-active` | Stable/static frontend |
| 18081 | file-transfer-18081 | `/root/basepro/fileTransfer` | File transfer service |
| 18082 | smallphone-stack | `/root/projects/smallphone/smallphone-active` | Beta/static frontend |
| 18096 | smallphone-stack | `/root/projects/smallphone/smallphone-active` | OpenCode backend |
| 4103 | smallphone-like-girl | `/root/projects/smallphone/smallphone-active` | LikeGirl standalone app |
| 4108 | smallphone-like-girl-clone | `/root/projects/smallphone/smallphone-active` | LikeGirl clone standalone app |

## Target Migration Map

| Current | Target | Service |
| --- | --- | --- |
| 8787 | 20087 | service-manager |
| 9810 | 21010 | cc-connect bridge |
| 9820 | 21020 | cc-connect management |
| 9840 | 21040 | cc-connect webclient/runtime facade |
| 3100 | 22000 | SmallPhone Core |
| 18080 | 22080 | SmallPhone stable frontend |
| 18081 | 22081 | file-transfer |
| 18082 | 22082 | SmallPhone beta frontend |
| 18096 | 22096 | OpenCode backend |
| 4103 | 23003 | LikeGirl |
| 4108 | 23008 | LikeGirl clone |

## Notes

- Port `9821` has no active listener or registered config on the current host.
- Keep this file updated when a service-manager registration changes.
- Do not document a target port as current until service-manager registrations,
  launch scripts, frontend defaults, health checks, and tests have migrated.
