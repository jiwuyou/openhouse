# Mode Config

## Summary

This file defines a user-editable mode model for the app. It is intended to let users choose how `daily`, `dev`, and `automation` workflows should behave without hard-coding one global policy.

The example JSON is provided at:

`config/mode-config.example.json`

## Design Goal

The configuration separates:

- visible foreground usage
- development workflow usage
- background automation usage

This avoids forcing one browser model to handle every scenario.

## Modes

### `daily`

Use this for normal browsing.

Recommended behavior:

- visible browser
- single main webview
- agent may run automation in the background
- automation results return by notification or control webview
- visible browser commands should default to `shared` session mode

### `dev`

Use this for website creation and debugging.

Recommended behavior:

- visible browser
- three-panel layout
- control webview
- terminal panel
- workspace webview
- shared session by default

### `automation`

Use this for background jobs.

Recommended behavior:

- headless browser
- no visible browser layout required
- isolated session by default
- results returned to terminal or notification

## Important Fields

### `default_mode`

The mode used when no explicit mode is selected.

### `browser_runtime`

Controls whether the mode expects:

- `visible`
- `headless`

### `layout`

Controls UI structure for the mode.

Supported example values:

- `single_webview`
- `three_panel`
- `none`

### `default_session_mode`

Controls login-state behavior.

Supported values:

- `isolated`
- `shared`

Recommended meaning:

- `isolated`: do not reuse cookies, local storage, or authenticated state from another mode
- `shared`: allow reuse of an existing visible session when the task explicitly needs it

### `target_context`

Defines where the command should execute.

Suggested values:

- `control_webview`
- `workspace_webview`
- `headless`

### `result_delivery`

Defines where the result should be returned.

Suggested values:

- `control_webview`
- `workspace_webview`
- `terminal`
- `notification`

## Session Profiles

`session_profiles` lets users define reusable session policies.

Example:

- `daily_shared`
- `dev_shared`
- `automation_isolated`

This is useful when a user wants automation tasks to sometimes reuse login state and sometimes stay isolated.

## Task Defaults

`task_defaults` lets users predefine behavior for common task categories.

Examples in the sample JSON:

- `open_url`
- `daily_background_job`
- `daily_background_job_shared_login`

This makes it possible to map user intent to a mode and session policy without changing code.

## Suggested Interpretation

Use the configuration as a contract:

- `daily` and `dev` are foreground user modes
- `automation` is a background execution mode
- session sharing is controlled by parameters, not forced globally

## Recommended Product Rules

- Keep `automation` isolated by default
- Only use `shared` session mode when a task explicitly needs an existing login state
- Keep `dev` focused on visible preview and debugging
- Keep `daily` focused on normal browsing, with optional background automation

## Current Status

The app now performs a minimal runtime load of the mode config during application startup.

Current runtime behavior:

- looks for `mode-config.json` in:
  - `~/.config/termux/mode-config.json`
  - `~/.termux/mode-config.json`
- falls back to an in-app default policy if no readable file exists
- exposes default mode and per-mode default policy values to runtime code
- `termux-browser` now accepts and forwards:
  - `--mode`
  - `--session-mode`
  - `--target-context`
  - `--result-delivery`
- request parsing falls back to runtime mode-config defaults when those fields are omitted
- request parsing can now apply a named `task_default` when `termux-browser --task-default <name>` is used
- disabled modes are now rejected at runtime
- `target_context` now affects command routing at runtime
- `default_mode`, `layout`, and `browser_runtime` now affect the initial `TermuxActivity` UI state
- `session_mode=isolated` is now explicitly rejected for visible-runtime execution, since true isolated browser sessions are not available yet in the current visible `WebView` architecture
- `result_delivery=notification` now produces an Android notification summary
- `result_delivery=control_webview` and `workspace_webview` now update visible browser panel status text when that panel is attached

Current limitations:

- the config is not yet fully wired into UI switching
- task defaults only apply when explicitly referenced by request
- session profiles are still metadata plus controller-key selection, not true isolated storage containers
- `result_delivery=terminal` is still just the shell command stdout path, not a second in-app delivery pipeline

This means the configuration model has now moved from "document only" to "loaded contract", but it is still in early integration stage.
