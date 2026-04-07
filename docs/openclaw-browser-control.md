# OpenClaw Browser Control Guide

## Purpose

This document tells OpenClaw how to control the browser in this project.

OpenClaw should treat `termux-browser` as the only supported browser control entrypoint.
It should not try to control Android UI widgets directly.

## Command Path

Preferred command path:

```bash
/data/user/0/com.termux/files/usr/bin/termux-browser
```

If `termux-browser` is already in `PATH`, the short form may be used:

```bash
termux-browser
```

## Default Runtime

Unless a task explicitly requires something else, OpenClaw should use:

```text
--mode dev --target-context workspace_webview --session-mode shared
```

Default target meaning:

- `mode=dev`: use the dev layout/runtime
- `target-context=workspace_webview`: use the workspace browser surface
- `session-mode=shared`: reuse the persistent browser session

## Output Contract

`termux-browser` returns JSON.

OpenClaw must treat the JSON as the source of truth:

- `ok=true`: command succeeded
- `ok=false`: command failed
- `result`: structured result payload
- `error.code` and `error.message`: failure reason

OpenClaw should not assume an action succeeded without checking the JSON response.

## Core Commands

Open a page:

```bash
termux-browser open https://example.com --mode dev --target-context workspace_webview --session-mode shared
```

Read visible text:

```bash
termux-browser read-text --mode dev --target-context workspace_webview --session-mode shared
```

Read DOM:

```bash
termux-browser read-dom --mode dev --target-context workspace_webview --session-mode shared
```

Click by CSS selector:

```bash
termux-browser click --selector "button" --mode dev --target-context workspace_webview --session-mode shared
```

Type into an input:

```bash
termux-browser type --selector "input[name=q]" --text "hello" --mode dev --target-context workspace_webview --session-mode shared
```

Read console:

```bash
termux-browser read-console --mode dev --target-context workspace_webview --session-mode shared
```

Take a screenshot when needed:

```bash
termux-browser screenshot --mode dev --target-context workspace_webview --session-mode shared
```

## Required Operating Pattern

OpenClaw should use this loop:

1. `open` a URL or reuse the current page.
2. `read-text` or `read-dom` to confirm the current page state.
3. Decide the next action from actual page content.
4. Run one action such as `click` or `type`.
5. Read the page again.

OpenClaw should avoid chaining multiple blind actions without a read step in between.

## Selector Policy

Prefer stable selectors in this order:

1. Semantic attributes like `name`, `type`, `value`
2. Stable ids
3. Short CSS selectors tied to form structure
4. Visible text only when no stable selector exists

Avoid brittle selectors that depend on deep layout nesting.

## Recommended Defaults

For normal browsing tasks:

```text
mode=dev
target-context=workspace_webview
session-mode=shared
```

For tasks that explicitly need the controller surface:

```text
mode=dev
target-context=control_webview
session-mode=shared
```

## Error Handling

If a command fails:

1. Read `error.code`
2. Read `error.message`
3. Re-check page state with `read-text` or `read-dom`
4. Retry only if the failure is clearly transient

Do not keep retrying the same failed action without reading state again.

## Example Workflow

Open a form page:

```bash
termux-browser open https://httpbin.org/forms/post --mode dev --target-context workspace_webview --session-mode shared
```

Confirm page content:

```bash
termux-browser read-text --mode dev --target-context workspace_webview --session-mode shared
```

Type a field:

```bash
termux-browser type --selector "input[name=custname]" --text "smoke-script" --mode dev --target-context workspace_webview --session-mode shared
```

Click a radio button:

```bash
termux-browser click --selector "input[name=size][value=medium]" --mode dev --target-context workspace_webview --session-mode shared
```

Submit:

```bash
termux-browser click --selector "button" --mode dev --target-context workspace_webview --session-mode shared
```

Verify result:

```bash
termux-browser read-text --mode dev --target-context workspace_webview --session-mode shared
```

## What OpenClaw Should Not Do

- Do not drive browser actions by tapping Android coordinates.
- Do not assume the foreground surface equals the command target.
- Do not assume a click or type succeeded without checking returned JSON.
- Do not invent unsupported browser commands.

## References

- Architecture rationale: `docs/architecture.md`
- Working command examples: `scripts/browser-smoke.ps1`
