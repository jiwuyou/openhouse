# Repository Map

## `/root/termux-src/termux-app`

OpenHouse APK project. This is the user-facing Android/Termux entry point.

Key files:

- `CONTRIBUTING_OPENHOUSE.md`
- `SECURITY_OPENHOUSE.md`
- `docs/OPENHOUSE.md`

APK output currently includes:

- `app/build/outputs/apk/debug/termux-app_apt-android-7-debug_universal.apk`
- split APKs for `x86_64`, `x86`, `armeabi-v7a`, and `arm64-v8a`

## `/root/openhouse-bootstrap`

Script-first installer and online maintenance source.

Key files:

- `bootstrap.sh`
- `scripts/40-install-opencode.sh`
- `scripts/42-install-codex.sh`
- `scripts/44-install-claude-code.sh`
- `openhouse-manifest.json`

## `/root/cc-connect-fresh`

OpenHouse Connect agent bridge and mobile web client.

Key files:

- `go.mod`
- `README.md`
- `cc-connect`
- `web/`
- `web2/`

## `/root/projects/cc-proxy`

Claude Code to OpenAI-compatible API proxy.

Key files:

- `Cargo.toml`
- `Cargo.lock`
- `crates/`
- `npm/`

## `/root/projects/openhouse-key-tool`

Manifest-driven API key replacement and verification tool.

Current role:

- Replace recorded API key targets after first-run setup.
- Back up changed files before replacement.
- Run manifest-defined smoke tests after replacement.
- Roll back on failed tests and keep logs for OpenCode troubleshooting.

Key files:

- `README.md`
- `examples/default.profile.json`
- `examples/cc-proxy.profile.json`
- `src/openhouse_key_tool/cli.py`
- `tests/test_cli.py`

## `/root/projects/smallphone/smallphone-active`

SmallPhone product app and integration workspace.

Key files and directories:

- `AGENTS.md`
- `smallphone-app/`
- `smallphone-user-shell-template/`
- `openclaw-smallphone-plugin/`

## `/root/projects/service-manager`

Local runtime control plane for Linux, macOS, and Termux/Ubuntu services.

Current role:

- Register services.
- Detect existing processes after restart.
- Start, stop, restart, and group-control local services.

## `/root/openhouse-app-guide-site`

Static usage guide site with screenshots.

Key files:

- `index.html`
- `styles.css`
- `assets/maintenance-center.png`
- `assets/maintenance-dynamic.png`
- `README.md`

## `/root/openhouse-docs`

Generated documentation site.

Key paths:

- `docs/`
- `site/`
- `scripts/build_docs_site.py`
