# Port Map

This document tracks local OpenHouse ports. Prefer `127.0.0.1` bindings unless
shared-device access requires a LAN or Tailscale address.

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

## Notes

- Port `9821` has no active listener or registered config on the current host.
- Keep this file updated when a service-manager registration changes.

