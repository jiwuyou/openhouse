# Ubuntu Bridge

## Summary

The recommended architecture is:

- Android app as the primary host
- Termux as the shell runtime host
- Ubuntu as an optional workload environment
- a bridge layer between Ubuntu and the Android/Termux host

This means Ubuntu should not replace the current Android host runtime.

## Why Use a Bridge Instead of Making Ubuntu the Host

The current product features already depend on Android-hosted capabilities:

- visible browser panels
- workspace webview
- control webview
- `termux-browser`
- runtime mode config

Ubuntu is useful for:

- package compatibility
- Linux userland tooling
- language runtimes and build tools

But Ubuntu is not the browser host.

## Bridge Entry Point

The host now exposes:

- `termux-browser`
- `termux-host`

Paths:

- browser CLI: `$PREFIX/bin/termux-browser`
- host bridge CLI: `$PREFIX/bin/termux-host`

In a bridged environment, Ubuntu should call the host bridge CLI instead of trying to own the browser runtime directly.

## Commands

### Browser Forwarding

```sh
termux-host browser open https://example.com --mode dev --target-context workspace_webview
```

This forwards directly into the host browser command path.

### Inspect Host Paths

```sh
termux-host paths
```

### Inspect Current Default Mode

```sh
termux-host mode show
```

## Intended Ubuntu Usage

Ubuntu can be used for:

- build tools
- package managers
- project commands
- scripting

Then it should call back into the host for:

- browser control
- visible preview
- host mode lookup

## Current Status

The bridge layer is now partially implemented:

- a stable host bridge CLI is generated
- host paths and default mode can be queried
- browser commands can be forwarded through the host bridge

This is still a minimal bridge.

What is not done yet:

- automatic Ubuntu environment setup
- automatic PATH injection for Ubuntu
- per-distro mount and environment bootstrap
- headless automation runtime integration

## Recommendation

Use Android + Termux as the system host.

Treat Ubuntu as a caller of host capabilities, not as the owner of browser capabilities.
