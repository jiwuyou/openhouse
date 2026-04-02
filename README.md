# openhouse

`openhouse` is an Android execution host built on top of the Termux app runtime.

It keeps the `com.termux` package identity for compatibility, while adding a visible in-app browser, a command bridge, and a workspace-oriented development flow for local and remote agents.

## What It Is

`openhouse` is not a generic Android browser and not a separate AI product.

It is a single-host Android app that combines:

- a Termux-compatible terminal runtime
- a visible browser runtime inside the app
- a command bridge exposed to the shell as `termux-browser`
- a shared workspace used by terminal and browser together

## Current Product Shape

The current implementation supports three runtime modes:

- `daily`
  - single visible browser for normal browsing and quick agent actions
- `dev`
  - three-surface workspace for preview, terminal visibility, and debugging
- `automation`
  - experimental headless browser path for background execution

The browser command bridge currently supports:

- `open`
- `click`
- `type`
- `scroll`
- `read-text`
- `read-dom`
- `read-console`
- `screenshot`

See [docs/termux-browser.md](./docs/termux-browser.md) for the current CLI contract.

## Repository Structure

- `app/`
  - Android application module
- `termux-shared/`
  - shared Termux libraries and utilities
- `terminal-emulator/`
  - terminal emulation core
- `terminal-view/`
  - terminal view widget
- `config/`
  - mode config examples
- `docs/`
  - product, runtime, and command bridge documentation

Important project docs:

- [docs/system-design.md](./docs/system-design.md)
- [docs/mode-config.md](./docs/mode-config.md)
- [docs/termux-browser.md](./docs/termux-browser.md)
- [docs/ubuntu-bridge.md](./docs/ubuntu-bridge.md)

## Build

Build a debug APK:

```sh
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

Artifacts are written under:

`app/build/outputs/apk/debug/`

## Install Notes

This project intentionally keeps the `com.termux` package name.

That means:

- app data still lives under the Termux paths
- existing scripts and tooling can keep using the same package-level paths
- signing compatibility rules for `com.termux` still apply

If a device already has another `com.termux` build installed from a different signing source, Android may reject installation until the old app and related plugin packages are removed.

## Browser Command Bridge

After bootstrap setup completes successfully, the app installs:

- `$PREFIX/bin/termux-browser`
- `$PREFIX/bin/termux-host`

Example commands:

```sh
termux-browser open https://example.com
termux-browser read-text
termux-browser screenshot
termux-browser open https://httpbin.org/forms/post --mode dev --target-context workspace_webview
termux-browser screenshot --mode dev --target-context workspace_webview
```

Downloads and screenshots currently default to:

`~/downloads`

## Compatibility Position

`openhouse` is best described as:

- a Termux-compatible Android host
- a visible browser + terminal workspace
- an execution base for local or SSH-connected agents

It is not yet a full replacement for all Termux distribution channels, plugins, or release practices.

## License

This repository is derived from the Termux app codebase and keeps the upstream license file in [LICENSE.md](./LICENSE.md).

When redistributing builds, review package-name, signing, and shared-user compatibility carefully.

## Upstream Base

This project is based on:

- <https://github.com/termux/termux-app>

Upstream remains the reference for core Termux runtime behavior; this fork adds the browser runtime, mode model, and command bridge on top.
