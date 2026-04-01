# Termux Browser CLI

## Summary

This fork adds an in-app browser runtime that can be driven from the Termux shell with the `termux-browser` command.

The browser host lives inside the main `TermuxActivity`, while the shell side writes command requests into the app workspace and triggers an exported receiver through `/system/bin/am`.

## Availability

`termux-browser` is installed into `$PREFIX/bin/termux-browser` after:

1. the app starts successfully, and
2. the Termux bootstrap has already been installed.

The shared workspace root is `~/`.

Downloads currently land in `~/downloads`.

## Supported Commands

```sh
termux-browser open https://example.com
termux-browser open https://example.com --task-default open_url
termux-browser click --selector "button.submit"
termux-browser type --selector "input[name=email]" --text "user@example.com"
termux-browser scroll --delta-y 800
termux-browser read-text
termux-browser read-dom
termux-browser read-console
termux-browser screenshot
termux-browser screenshot --output ~/downloads/example-page.png
```

Each command prints JSON to stdout and exits non-zero on failure.

Additional request routing flags:

- `--task-default <name>`
- `--mode <daily|dev|automation>`
- `--session-mode <isolated|shared>`
- `--session-profile <name>`
- `--share-from <context>`
- `--target-context <control_webview|workspace_webview|headless>`
- `--result-delivery <csv>`
- `--output <path>` for `screenshot`

## Current Behavior

- The browser is a single persistent in-app `WebView`.
- Cookies and `LocalStorage` are preserved by the WebView runtime.
- File uploads use the app's `DocumentsProvider`, rooted at `~/`.
- Browser downloads are saved into `~/downloads`.
- `screenshot` captures the current browser `WebView` viewport to a PNG inside the Termux workspace.
- If `--output` is omitted, screenshots are saved into `~/downloads`.
- Console errors are collected and returned by `read-console`.
- `task_defaults` from mode config can now populate mode/session/context defaults when `--task-default` is used.
- `result_delivery=notification` posts a Termux notification with a compact command outcome summary.
- `result_delivery=control_webview` or `workspace_webview` updates the corresponding visible browser panel status when available.
- `daily` visible-browser commands now default to `shared` session mode so the out-of-box command path works on foreground webviews.

## Current Limits

- No multi-tab support
- Screenshot capture is currently limited to visible browser runtimes; headless `automation` does not support it yet
- No screen recording
- No dedicated socket server yet; the current command bridge uses request/response directories plus `/system/bin/am`
- `terminal` result delivery is still satisfied by CLI stdout rather than a separate in-app terminal delivery channel
- true isolated browser session containers are not implemented yet; `isolated` is only meaningful for the current headless runtime selection path

## Build Verification

The current debug build was verified with:

```sh
gradlew.bat :app:assembleDebug
```

Artifacts are written under:

`app/build/outputs/apk/debug/`
