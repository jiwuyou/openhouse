# Package Name Coexistence Test Checklist

## Purpose

This checklist is for verifying that a renamed package build can coexist with official `Termux`.

Primary goal:

- both apps can be installed on the same device
- both apps can launch and run independently
- this fork still keeps its terminal, browser, file-provider, and automation features working

Related documents:

- [package-name-coexistence-scope.md](/C:/Users/24045/termuweb/docs/package-name-coexistence-scope.md)
- [package-name-coexistence-checklist.md](/C:/Users/24045/termuweb/docs/package-name-coexistence-checklist.md)

## Test Matrix

Run the checklist on at least:

1. A clean device without official Termux installed
2. A device with official Termux already installed
3. A device where this fork was previously installed under the old package identity

If only one real device is available, prioritize case 2.

## Phase 1: Install Identity

### 1. APK metadata sanity

Before installing, verify:

- APK package name is the new package, not `com.termux`
- app label and icon are correct
- manifest authorities no longer point to `com.termux.*`

Suggested checks:

- `aapt dump badging <apk>`
- Android Studio APK Analyzer

Expected result:

- package identity is new
- no install-time identity collisions with official Termux

### 2. Side-by-side install with official Termux

Steps:

1. Install official Termux
2. Install the renamed fork APK

Expected result:

- both apps install successfully
- neither app replaces the other
- launcher shows two separate apps

### 3. Upgrade path for renamed fork

Steps:

1. Install one build of the renamed fork
2. Install a newer build with the same renamed package

Expected result:

- upgrade succeeds
- app data is retained
- app does not create a duplicate launcher entry

## Phase 2: Basic Launch

### 4. Launch each app independently

Steps:

1. Launch official Termux
2. Launch the renamed fork
3. Switch back and forth

Expected result:

- both apps open normally
- each app returns to its own task
- launching one does not foreground the other

### 5. Launcher shortcuts

Steps:

1. Long-press the renamed fork icon
2. Check available app shortcuts
3. Trigger each shortcut

Expected result:

- shortcuts point to the renamed fork
- `New session` opens the renamed fork
- `Settings` opens the renamed fork settings activity

## Phase 3: Terminal Runtime

### 6. Bootstrap and shell startup

Steps:

1. Launch the renamed fork on a fresh install
2. Wait for bootstrap/setup to finish
3. Verify shell prompt appears

Expected result:

- bootstrap completes
- terminal opens normally
- app uses its own private data directory

### 7. Package manager basic commands

Run in the renamed fork:

```bash
echo ok
pwd
whoami
pkg --version
```

Expected result:

- shell works
- prefix paths are correct for the renamed package
- no hidden dependency on `/data/data/com.termux/...`

### 8. Filesystem paths

Run in the renamed fork:

```bash
echo $PREFIX
echo $HOME
```

Expected result:

- paths resolve under the renamed package data directory
- no leakage into official Termux storage path

## Phase 4: Isolation From Official Termux

### 9. Data isolation

Steps:

1. Create a file in official Termux home
2. Create a different file in renamed fork home
3. Compare visibility

Expected result:

- each app sees its own private app home by default
- no accidental cross-package home sharing

### 10. Notification isolation

Steps:

1. Start long-running sessions in both apps
2. Check notifications

Expected result:

- both apps show separate notifications
- notification channels and actions work independently

### 11. Settings isolation

Steps:

1. Change a setting in official Termux
2. Check the renamed fork
3. Change a setting in the renamed fork
4. Check official Termux

Expected result:

- settings do not overwrite each other
- shared preferences names and files are isolated as intended

## Phase 5: File Provider And Document Access

### 12. Documents provider registration

Steps:

1. Open Android system file picker
2. Check providers list

Expected result:

- the renamed fork provider appears as a separate provider
- official Termux provider still exists if installed

### 13. Upload flow

Steps:

1. In the renamed fork browser, trigger file upload
2. Select a file via DocumentsUI

Expected result:

- provider authority for the renamed app works
- upload completes

### 14. Open-with and share flow

Steps:

1. Share a file to the renamed fork
2. Open a file with the renamed fork as viewer

Expected result:

- intents resolve to the renamed fork
- file receiver activity works

## Phase 6: Browser Runtime

### 15. Daily mode browser open/read

Run:

```bash
termux-browser open https://example.com
termux-browser read-text
```

Expected result:

- browser opens page
- page text is returned correctly

### 16. Dev workspace browser open/read

Run:

```bash
termux-browser open https://httpbin.org/forms/post --mode dev --target-context workspace_webview --session-mode shared
termux-browser read-text --mode dev --target-context workspace_webview --session-mode shared
```

Expected result:

- dev mode still works
- workspace browser surface is routed correctly

### 17. Browser localhost access

Run a local HTTP server inside the renamed fork, then test:

```bash
termux-browser open http://127.0.0.1:18080
termux-browser read-text
```

Expected result:

- app-internal browser can still access its own localhost service

## Phase 7: Browser Command Bridge

### 18. Browser CLI path

Run:

```bash
command -v termux-browser
```

Expected result:

- CLI exists
- CLI works from the renamed package runtime

### 19. Browser command receiver and response files

Verify:

- request directories are created
- response directories are created
- browser commands return JSON

Expected result:

- package rename did not break request/response routing

## Phase 8: External Command / Automation Interfaces

### 20. `run-as` based automation

Steps:

1. Update automation scripts to the new package
2. Run smoke scripts

Expected result:

- `run-as <new.package>` works
- old `run-as com.termux` is no longer used for the renamed fork

### 21. Activity launch via adb

Test:

```bash
adb shell am start -n <new.package>/.app.TermuxActivity
```

Expected result:

- renamed app launches
- old component name is not required

### 22. Browser smoke script

Run the project smoke script after patching package references.

Expected result:

- script passes against the renamed fork

## Phase 9: SSH

### 23. SSH server startup

Inside the renamed fork:

```bash
sshd
```

Expected result:

- SSH server starts under the renamed package runtime

### 24. SSH login

From host:

```bash
ssh -p <port> <user>@<device>
```

Expected result:

- host can log in to the renamed fork runtime
- official Termux SSH setup is not disturbed

## Phase 10: Plugin/Integration Compatibility

### 25. RUN_COMMAND permission and action

Verify:

- custom permission name changed consistently
- action string changed consistently
- app-side command execution still works

Expected result:

- no leftover `com.termux.*` permission/action mismatch

### 26. Plugin package assumptions

If plugin support is still desired, verify:

- API app package names
- widget/tasker/float/styling references

Expected result:

- either plugin ecosystem is intentionally preserved with matching renamed packages
- or it is intentionally disabled and documented

## Phase 11: Regression Checks

### 27. Existing docs/scripts no longer point to old package

Audit:

- scripts
- docs
- helper commands

Expected result:

- operator instructions for the renamed fork use the new package identity

### 28. No accidental official-Termux breakage

With official Termux still installed:

1. Launch official Termux
2. Run a simple shell command
3. Open a local file
4. Verify notifications/settings still work

Expected result:

- official Termux remains unaffected

## Exit Criteria

The package rename can be considered acceptable for coexistence when all are true:

1. Official Termux and the renamed fork install side by side
2. Both launch independently
3. Terminal runtime works in the renamed fork
4. Browser runtime and `termux-browser` still work
5. File provider and upload/download flows still work
6. Automation scripts work after package reference updates
7. SSH still works in the renamed fork
8. Official Termux behavior is not regressed
