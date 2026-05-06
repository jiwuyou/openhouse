# OpenHouse System Map

OpenHouse is a mobile-first AI work environment built around Termux/OpenHouse
App, local agent runtimes, and a service control plane.

## Layers

1. Entry layer: OpenHouse APK
2. Install layer: OpenHouse Bootstrap
3. Runtime control layer: service-manager
4. Agent bridge layer: cc-connect
5. Protocol adapter layer: cc-proxy
6. Product app layer: SmallPhone and future apps
7. Documentation layer: guide site and docs site

## Runtime Shape

```text
OpenHouse APK
  -> openhouse-bootstrap installs and configures the environment
  -> service-manager starts and controls local services
  -> cc-connect exposes agent bridge and mobile web client
  -> cc-proxy adapts Claude Code to OpenAI-compatible APIs
  -> smallphone consumes runtime services as an application
```

## Design Decision

OpenHouse should not become a monorepo. The top-level repository coordinates
decisions and integration. Each child repository keeps ownership of its own
source code, release process, and tests.

