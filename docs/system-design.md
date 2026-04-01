# System Design

## Summary

This document defines the overall product and runtime design for the Android execution base after the initial implementation and real-device validation work.

The current conclusion is that the product should not force one browser model to handle every scenario.

Instead, the system should be split into:

- foreground user modes
- background automation execution
- configuration-driven session and routing rules

## Design Drivers

The design is driven by the following validated observations:

1. A visible in-app browser is valuable and already works for real-device foreground flows.
2. A foreground browser is sufficient for development and preview workflows.
3. Background automation should not depend on the visible `WebView` host.
4. Session sharing should be user-controlled, not hard-coded globally.
5. Normal browsing, development, and automation are different product jobs and should not be collapsed into one runtime mode.

## Mode Model

The system is divided into three modes.

### `daily`

Purpose:

- normal browsing
- user-facing browsing experience
- optional automation requests triggered from user context

Characteristics:

- visible browser
- simple layout
- user-first interaction
- agent may run background automation jobs without taking over the visible page

### `dev`

Purpose:

- website creation
- preview
- debugging
- collaborative agent workflow

Characteristics:

- visible browser
- three-panel layout
- control webview
- terminal panel
- workspace webview
- user and agent can both observe active work

### `automation`

Purpose:

- background job execution
- website automation
- multi-step scripted flows
- tasks that should not require a foreground visible browser

Characteristics:

- headless browser
- background execution engine
- can be called from `daily` or `dev`
- should not depend on foreground `Activity` lifecycle

## Core Product Structure

The product should be treated as:

- two foreground user modes:
  - `daily`
  - `dev`
- one background execution mode:
  - `automation`

This means:

- `daily` and `dev` define what the user sees
- `automation` defines what the agent executes

## Runtime Layers

### 1. Shell Runtime

Responsibilities:

- Termux-compatible shell execution
- file operations
- package operations
- SSH access
- command entrypoint for agent requests

This layer already exists and remains the base execution layer.

### 2. Visible Browser Runtime

Responsibilities:

- user-visible browser
- page preview
- DOM inspection
- interactive debugging
- input, click, scroll, read-text, read-dom, read-console

This layer is used by:

- `daily`
- `dev`

This layer is not sufficient on its own for fully background automation.

### 3. Headless Automation Runtime

Responsibilities:

- background browser automation
- automation tasks that continue even when the visible UI is not actively used
- tasks triggered from `daily`
- tasks triggered from `dev`

This layer should be the long-term target for:

- no-UI automation
- scheduled jobs
- multi-step agent-run workflows

### 4. Session and Context Manager

Responsibilities:

- manage shared and isolated browser sessions
- decide whether an automation task reuses current login state
- bind tasks to the correct execution context

This layer must be configuration-driven.

## UI Structure

### Daily Mode UI

Recommended UI:

- single visible browser
- optional small agent affordance
- minimal interruption

Primary goal:

- browsing first

### Dev Mode UI

Recommended UI:

- control webview
- terminal panel
- workspace webview

Recommended interpretation:

- control webview: user-agent interaction surface
- terminal panel: command and task visibility
- workspace webview: current site under development

Primary goal:

- collaborative development and debugging

### Automation Mode UI

Recommended UI:

- no dedicated visible UI required
- optional notifications
- optional task inspector page

Primary goal:

- execution, not direct display

## Context Routing Model

Every command should resolve to a target execution context.

Suggested contexts:

- `control_webview`
- `workspace_webview`
- `headless`

Examples:

- `daily` browsing helper action may target `headless`
- `dev` preview action may target `workspace_webview`
- `dev` assistive lookup action may target `control_webview`

This routing decision should not be hidden in code branches only.
It should be made explicit in configuration and command metadata.

## Session Model

Session behavior should be parameterized.

Supported session intent:

- `isolated`
- `shared`

Meaning:

- `isolated`: automation gets a separate cookie/storage/login state
- `shared`: automation reuses the chosen visible session or profile

Recommended rule:

- default to `isolated`
- allow `shared` only when a task explicitly requires existing login state

Future-safe extension:

- `profile:<id>`

This would allow stable reusable session containers without forcing everything into one shared state.

## Why Session Control Must Be Explicit

Without explicit session control:

- automation can pollute daily browsing
- development workflows can inherit unintended login state
- debugging becomes difficult
- user trust drops because behavior becomes unpredictable

So session sharing must be:

- intentional
- visible in configuration
- routable per task

## Result Delivery Model

Automation results should not always return to the same place.

Suggested result targets:

- `control_webview`
- `workspace_webview`
- `terminal`
- `notification`

Examples:

- a daily automation task may return to `notification`
- a dev automation task may return to `workspace_webview` and `terminal`
- a debug task may return to `terminal` and `control_webview`

## Configuration Role

The user-editable JSON configuration should define:

- enabled modes
- default mode
- visible layout choice
- runtime type
- default session behavior
- default target context
- result delivery targets
- reusable session profiles
- common task defaults

This means the config is not just cosmetic.
It becomes the policy layer of the app.

## Current Implementation Status

Validated today:

- foreground `TermuxActivity` browser flow works on real device
- `open`
- `read-text`
- `read-dom`
- `read-console`
- `type`
- `scroll`
- `click` leading to link navigation
- `click` leading to form submission

Also validated:

- command execution from Termux shell can drive the visible in-app browser

Current limitation:

- local shared-workspace preview is not fully solved yet
- background browser automation should not keep depending on the visible `WebView`

## Current Architectural Gap

The current foreground browser implementation is good enough for:

- `dev`
- foreground `daily` interactions

But it is not yet the correct long-term runtime for:

- `automation`

Therefore, the next architectural split should be:

- keep the visible `WebView` host for `daily` and `dev`
- introduce a separate headless automation execution layer for `automation`

## Recommended Implementation Order

### Phase 1

Stabilize the current visible browser flow:

- make command behavior deterministic
- stabilize DOM and navigation timing
- improve selector behavior
- improve local preview handling

### Phase 2

Implement config loading and routing:

- parse mode config JSON
- resolve mode
- resolve session mode
- resolve target context
- resolve result delivery

### Phase 3

Split execution contexts:

- visible browser runtime for `daily` and `dev`
- headless browser runtime for `automation`

### Phase 4

Add session container support:

- isolated sessions
- shared sessions
- optional named profiles

### Phase 5

Add task inspection and operational polish:

- running job visibility
- result history
- automation logs

## Design Recommendation

Do not treat background automation as a special case of the visible browser.

Treat it as a separate runtime mode that may optionally borrow session state.

Do not treat `daily`, `dev`, and `automation` as only visual presets.

Treat them as operational modes with different:

- runtime types
- context routing
- session policies
- result targets

## Immediate Next Work

The next engineering step should be one of:

1. connect the mode config JSON to runtime policy resolution
2. design the headless automation runtime boundary
3. finish local shared-workspace preview for the visible browser flow

Current recommendation:

Start with:

- runtime policy loading
- local shared-workspace preview

Then move to:

- headless automation runtime
