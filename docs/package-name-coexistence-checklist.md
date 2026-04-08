# Package Name Coexistence Checklist

## Goal

This checklist is for making this app installable alongside official `Termux`.

That means:

- official Termux keeps package identity `com.termux`
- this fork must use a different Android package identity

This document is the action-oriented companion to:

- [package-name-coexistence-scope.md](/C:/Users/24045/termuweb/docs/package-name-coexistence-scope.md)

## Recommended Strategy

Do not start by renaming every Java package.

Start with:

1. Android install identity
2. shared runtime constants
3. scripts and automation
4. resource XML with hardcoded package/class references

Only after that:

5. optionally rename source package paths

## Phase 1: Install Identity

### File: `app/build.gradle`

Path:

- [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)

Current package-coupled lines:

- `namespace "com.termux"`
- `manifestPlaceholders.TERMUX_PACKAGE_NAME = "com.termux"`

Action:

- choose a new app package name
- update `namespace`
- update `manifestPlaceholders.TERMUX_PACKAGE_NAME`
- if you decide to add explicit `applicationId`, keep it aligned with the same new package

Risk:

- many manifest identities are derived from `TERMUX_PACKAGE_NAME`
- if Gradle and shared constants diverge, startup and command helpers will break

### File: `app/src/main/AndroidManifest.xml`

Path:

- [AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)

Action:

Review every `${TERMUX_PACKAGE_NAME}`-derived identity, especially:

- `android:sharedUserId`
- custom permission names
- `provider` authorities
- `RUN_COMMAND` action
- `taskAffinity`

High-priority entries:

- `${TERMUX_PACKAGE_NAME}.permission.RUN_COMMAND`
- `${TERMUX_PACKAGE_NAME}.documents`
- `${TERMUX_PACKAGE_NAME}.files`
- `${TERMUX_PACKAGE_NAME}.RUN_COMMAND`

Risk:

- `sharedUserId` is a special risk item for coexistence and may need removal or redesign
- even if install works, mismatched authorities/actions will break file provider and command bridges

## Phase 2: Shared Runtime Constants

### File: `termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java`

Path:

- [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)

This is the most important code file for package rename work.

The file itself already documents the migration surface.

### Required edits in `TermuxConstants.java`

1. Core identity constants

- `TERMUX_PACKAGE_NAME`
- `TERMUX_APP_NAME`

2. Package-derived plugin constants

- `TERMUX_API_PACKAGE_NAME`
- `TERMUX_BOOT_PACKAGE_NAME`
- `TERMUX_FLOAT_PACKAGE_NAME`
- `TERMUX_STYLING_PACKAGE_NAME`
- `TERMUX_TASKER_PACKAGE_NAME`
- `TERMUX_WIDGET_PACKAGE_NAME`

3. Filesystem paths tied to package name

- `TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH`
- `TERMUX_FILES_DIR_PATH`
- `TERMUX_PREFIX_DIR_PATH`
- `TERMUX_HOME_DIR_PATH`
- `TERMUX_APPS_DIR_PATH`

4. Preference and file-provider constants

- `TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`
- `TERMUX_FILE_SHARE_URI_AUTHORITY`

5. App component class-name constants

- `BUILD_CONFIG_CLASS_NAME`
- `FILE_SHARE_RECEIVER_ACTIVITY_CLASS_NAME`
- `FILE_VIEW_RECEIVER_ACTIVITY_CLASS_NAME`
- `BROWSER_COMMAND_RECEIVER_CLASS_NAME`
- `TERMUX_ACTIVITY_NAME`
- `TERMUX_SETTINGS_ACTIVITY_NAME`
- `TERMUX_SERVICE_NAME`
- `RUN_COMMAND_SERVICE_NAME`

6. Intent action / extra names derived from package name

- `BROADCAST_TERMUX_OPENED`
- `PERMISSION_RUN_COMMAND`
- `RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND`
- all `TERMUX_APP.TERMUX_SERVICE.*` strings based on `TERMUX_PACKAGE_NAME`
- all `TERMUX_APP.RUN_COMMAND_SERVICE.*` strings based on `TERMUX_PACKAGE_NAME`

Risk:

- this file also controls many path assumptions in bootstrap/runtime
- package rename without matching path/runtime treatment can break binaries, shell integration, and app startup

## Phase 3: Resource XML That Must Be Manually Patched

### File: `app/src/main/res/xml/shortcuts.xml`

Path:

- [shortcuts.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/shortcuts.xml)

Why it matters:

This file already contains a warning comment saying package changes require manual patching.

Current hardcoded values:

- `android:targetPackage="com.termux"`
- `android:targetClass="com.termux.app.TermuxActivity"`
- `android:targetClass="com.termux.app.activities.SettingsActivity"`
- extra name `com.termux.app.failsafe_session`

Action:

- patch target package
- patch target class names
- patch hardcoded extra names if they remain package-coupled

### Files: preference XML resources

Directory:

- `app/src/main/res/xml/*.xml`

Examples:

- [root_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/root_preferences.xml)
- [termux_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/termux_preferences.xml)
- [termux_api_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/termux_api_preferences.xml)
- [termux_float_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/termux_float_preferences.xml)
- [termux_tasker_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/termux_tasker_preferences.xml)
- [termux_widget_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/termux_widget_preferences.xml)

Action:

- audit for hardcoded package strings
- audit for preference filenames/keys derived from package
- audit any documentation summary text that still points users to `com.termux` paths or plugins

Risk:

- some XML resources cannot rely on manifest placeholder substitution
- this is explicitly called out inside `TermuxConstants.java`

## Phase 4: Scripts And Automation

### Directory: `scripts/`

Confirmed high-priority file:

- [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)

Known hardcoded patterns to patch:

- `run-as com.termux`
- `/data/user/0/com.termux/...`
- `com.termux/.app.TermuxActivity`

Action:

- replace hardcoded runtime package with the new package name
- update adb launch targets
- update any path assumptions under `/data/user/0/...`

Why it matters:

- even if the APK installs, your smoke tests and browser automation will immediately break if these stay unchanged

## Phase 5: App Code Audit

### Directory: `app/src/main/java/**`

Action:

- search for hardcoded `com.termux`
- search for references to package-derived authorities/actions
- verify no runtime command wrappers still assume the old package

Important note:

- this does not necessarily mean renaming all source packages yet
- it means auditing hardcoded install/runtime identity usage

### Directory: `termux-shared/src/main/java/**`

Action:

- search for direct string comparisons against `com.termux`
- search for path construction that assumes `/data/data/com.termux`
- search for plugin package assumptions

This module should be treated as required audit scope, not optional.

## Phase 6: Tests

### Directories

- `app/src/test/**`
- `termux-shared/src/androidTest/**`
- any other instrumentation test tree

Action:

- patch any hardcoded package names
- patch authorities and action names
- patch expected paths

Reason:

- tests often encode exactly the same assumptions as scripts

## Phase 7: Docs And Operator Instructions

### Directory: `docs/`

Action:

- patch any instructions using:
  - `run-as com.termux`
  - `/data/user/0/com.termux`
  - `/data/data/com.termux`
  - `com.termux/.app.TermuxActivity`

Reason:

- stale docs become operational bugs during install/QA/SSH flows

## Specific Search Checklist

Search these strings across the repo:

```text
com.termux
/data/user/0/com.termux
/data/data/com.termux
run-as com.termux
com.termux/.app.TermuxActivity
TERMUX_PACKAGE_NAME
permission.RUN_COMMAND
.documents
.files
.RUN_COMMAND
```

## What Can Usually Wait

These are not the first blockers for coexistence:

- renaming `package com.termux...` declarations in every Java/Kotlin file
- moving source directories from `com/termux/...` to a new tree
- cosmetic doc cleanup unrelated to install/runtime identity

These may still be desirable later, but they are not phase-1 blockers.

## Practical First Pass

If you want the fastest first attempt at coexistence, do this first:

1. Patch [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)
2. Patch [AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)
3. Patch [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
4. Patch [shortcuts.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/shortcuts.xml)
5. Patch [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)
6. Rebuild
7. Verify install alongside official Termux
8. Only then continue deeper cleanup

## Bottom Line

For coexistence, the smallest real checklist is not “change a few strings”.

It is:

- Gradle identity
- manifest-derived Android identity
- shared constants
- script/runtime package references
- manually patched XML resources

Those are the files and areas most likely to block successful coexistence.
