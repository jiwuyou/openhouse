# 2026-05 OpenHouse Top-Level Repository

## Goal

Create a top-level coordination repository for OpenHouse that keeps global
context without merging child repositories.

## Decisions

- Keep implementation in child repositories.
- Keep architecture, plans, port maps, and service-manager conventions here.
- Use service-manager as the runtime control plane.
- Use `group:<name>` tags for service grouping.

## Initial Child Repository Set

- `/root/termux-src/termux-app`
- `/root/openhouse-bootstrap`
- `/root/cc-connect-fresh`
- `/root/projects/cc-proxy`
- `/root/projects/smallphone/smallphone-active`
- `/root/projects/service-manager`
- `/root/openhouse-app-guide-site`
- `/root/openhouse-docs`

## Next Work

- Add a real integration test runner after the service-manager API stabilizes.
- Decide whether guide-site and docs-site should share a single published entry.
- Register cc-proxy in service-manager if it should be part of the default stack.

