# Release Flow

OpenHouse has multiple release units. Keep each release owned by its repository.

## Release Units

| Unit | Repository | Output |
| --- | --- | --- |
| APK | `/root/termux-src/termux-app` | Android APK files |
| Bootstrap | `/root/openhouse-bootstrap` | Shell scripts and manifest |
| Connect | `/root/cc-connect-fresh` | Go binary and web assets |
| Proxy | `/root/projects/cc-proxy` | Rust binary / npm package |
| Service Manager | `/root/projects/service-manager` | Rust binary and Web UI |
| Guide Site | `/root/openhouse-app-guide-site` | Static site |
| Docs Site | `/root/openhouse-docs` | Generated static site |

## Release Rule

Do not publish a cross-repository change until each changed child repository has
its own tests or checks run. Record cross-repository release notes here when a
feature spans more than one repository.

