# Install Contract

OpenHouse keeps implementation in child repositories. Each child repository owns
how it installs, checks, upgrades, and registers itself. The APK maintenance
center and `openhouse-bootstrap` should orchestrate those entry points instead
of embedding child repository internals.

## Rule

Do not copy child repository install logic into the APK or this coordination
repository. Put install logic in the repository that owns the component, then
call it from bootstrap or the maintenance center.

## Standard Entry Points

Runtime repositories should provide these scripts when applicable:

| Script | Purpose |
| --- | --- |
| `scripts/install.sh` | Idempotently install runtime dependencies and the component itself. |
| `scripts/check.sh` | Read-only verification that reports whether the component is usable. |
| `scripts/register-service.sh` | Register service-manager records for long-running services. |
| `scripts/uninstall.sh` | Remove installed binaries or generated service records when supported. |

Scripts must be safe to run more than once. They must not write secrets to
tracked files, logs, or shared documentation.

## Repository Responsibilities

| Repository | Role | Install responsibility |
| --- | --- | --- |
| `/root/termux-src/termux-app` | APK entry point and maintenance center | Build and ship the APK. Expose maintenance actions, but do not own child runtime install internals. |
| `/root/openhouse-bootstrap` | Install orchestrator and online maintenance source | Call each child repository install/check entry point in product order. |
| `/root/cc-connect-fresh` | Agent bridge and mobile web client | Install or build `cc-connect`, initialize config, and register bridge/webclient services. |
| `/root/projects/cc-proxy` | Claude Code to OpenAI-compatible proxy | Install `cc-proxy` from npm, release binary, or source build mode, then register the proxy service. |
| `/root/projects/openhouse-key-tool` | API key replacement and verification tool | Install the Python CLI and provide smoke checks for profile replacement. |
| `/root/projects/smallphone/smallphone-active` | SmallPhone product app stack | Install Node package dependencies, prepare data directories, and register app services. |
| `/root/projects/service-manager` | Local service control plane | Install the `service-manager` binary and initialize config/token storage. |
| `/root/openhouse-app-guide-site` | Static guide site | Provide static-site sync or publish instructions; no runtime dependency is required. |
| `/root/openhouse-docs` | Generated documentation site | Install docs build dependencies only when building or publishing docs. |

## Bootstrap Flow

The intended runtime install chain is:

```text
APK maintenance center
  -> openhouse-bootstrap
      -> service-manager install/check
      -> cc-connect install/check/register-service
      -> cc-proxy install/check/register-service
      -> openhouse-key-tool install/check
      -> smallphone install/check/register-service
      -> guide/docs sync or build steps when requested
```

Build-only toolchains such as Android Gradle, Go, Rust, and documentation
publishing dependencies should stay out of the default phone runtime install
unless a user explicitly chooses a developer/source-build stage.

