# OpenHouse

OpenHouse is the top-level coordination repository for the OpenHouse product
system.

This repository does not vendor or copy source code from child repositories.
It keeps the cross-repository context: architecture, repository boundaries,
ports, service-manager registrations, integration plans, and release flow.

## Child Repositories

- `/root/termux-src/termux-app` - OpenHouse APK / Termux app fork.
- `/root/openhouse-bootstrap` - bootstrap installer and online maintenance source.
- `/root/cc-connect-fresh` - OpenHouse Connect agent bridge and mobile web client.
- `/root/projects/cc-proxy` - Claude Code to OpenAI-compatible proxy.
- `/root/projects/smallphone/smallphone-active` - SmallPhone application stack.
- `/root/projects/service-manager` - local service control plane.
- `/root/openhouse-app-guide-site` - screenshot-led static usage guide.
- `/root/openhouse-docs` - generated documentation site.

## Working Model

Open Codex from this repository when doing cross-project work. Use the docs in
this repo to decide what needs to change, then edit the relevant child
repository directly.

Keep implementation inside the owning child repository. Keep decisions,
integration notes, and cross-repository plans here.

## Key Docs

- [System map](docs/system-map.md)
- [Repository map](docs/repositories.md)
- [Ports](docs/ports.md)
- [Service manager](docs/service-manager.md)
- [Bootstrap flow](docs/bootstrap-flow.md)
- [Release flow](docs/release-flow.md)
- [Integration tests](docs/integration-tests.md)

