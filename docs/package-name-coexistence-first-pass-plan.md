# Package Name Coexistence First Pass Plan

## Purpose

This document defines the first practical change batch for making this fork coexist with official `Termux`.

Goal of this phase:

- change Android install identity
- keep the app bootable
- keep scripts and automation adaptable
- avoid a full source-package refactor in the first pass

This is not the final cleanup pass.

Related:

- [package-name-coexistence-scope.md](/C:/Users/24045/termuweb/docs/package-name-coexistence-scope.md)
- [package-name-coexistence-checklist.md](/C:/Users/24045/termuweb/docs/package-name-coexistence-checklist.md)
- [package-name-coexistence-test-checklist.md](/C:/Users/24045/termuweb/docs/package-name-coexistence-test-checklist.md)

## Proposed First-Pass Principle

Do not rename every `package com.termux...` declaration yet.

Instead:

1. change install/runtime package identity
2. update package-derived constants
3. patch manually hardcoded XML and scripts
4. run coexistence validation

This reduces churn and gives faster signal on whether coexistence is viable.

## Working Assumption

Assume the new package name will be something like:

```text
com.openhouse.app
```

Replace with the real final package later.

The checklist below uses `<NEW_PACKAGE>` as a placeholder.

## Batch 1 Files

### 1. `app/build.gradle`

Path:

- [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)

Suggested first-pass edits:

- change:
  - `namespace "com.termux"`
  - `manifestPlaceholders.TERMUX_PACKAGE_NAME = "com.termux"`

Suggested target:

- `namespace "<NEW_PACKAGE>"`
- `manifestPlaceholders.TERMUX_PACKAGE_NAME = "<NEW_PACKAGE>"`

Optional but recommended:

- add explicit `applicationId "<NEW_PACKAGE>"` inside `defaultConfig`

Why:

- this makes the install identity explicit
- avoids relying only on namespace behavior

### 2. `app/src/main/AndroidManifest.xml`

Path:

- [AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)

Suggested first-pass edits:

- re-evaluate `android:sharedUserId="${TERMUX_PACKAGE_NAME}"`

First-pass recommendation:

- remove `sharedUserId` unless you have a very specific reason to preserve it

Why:

- it is a high-risk legacy mechanism
- coexistence with official Termux should not depend on sharing the old package-linked user identity

Keep placeholder-derived identities, but expect them to change automatically once `TERMUX_PACKAGE_NAME` changes:

- custom permission
- content provider authorities
- `RUN_COMMAND` action
- task affinity

Manual review items after placeholder change:

- all exported authorities
- all custom permission names
- all action strings

### 3. `termux-shared/.../TermuxConstants.java`

Path:

- [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)

This is the primary code file to patch in batch 1.

Suggested first-pass edits:

#### Core identity

- `TERMUX_PACKAGE_NAME = "<NEW_PACKAGE>"`
- decide whether `TERMUX_APP_NAME` remains branding-only or should also change

#### Package-derived plugin names

These will recalculate automatically once `TERMUX_PACKAGE_NAME` changes, but must be reviewed:

- `TERMUX_API_PACKAGE_NAME`
- `TERMUX_BOOT_PACKAGE_NAME`
- `TERMUX_FLOAT_PACKAGE_NAME`
- `TERMUX_STYLING_PACKAGE_NAME`
- `TERMUX_TASKER_PACKAGE_NAME`
- `TERMUX_WIDGET_PACKAGE_NAME`

#### Filesystem paths

These are package-derived and must be expected to change:

- `TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH`
- `TERMUX_FILES_DIR_PATH`
- `TERMUX_PREFIX_DIR_PATH`
- `TERMUX_HOME_DIR_PATH`
- `TERMUX_APPS_DIR_PATH`

Important warning:

This file itself notes that bootstrap/runtime assumptions are tied to package name and private app data path.

So first-pass validation must explicitly test:

- bootstrap
- shell startup
- `$PREFIX`
- `$HOME`

#### Authorities and actions

Review all package-derived action/authority constants:

- `PERMISSION_RUN_COMMAND`
- `BROADCAST_TERMUX_OPENED`
- `TERMUX_FILE_SHARE_URI_AUTHORITY`
- `TERMUX_ACTIVITY_NAME`
- `RUN_COMMAND_SERVICE_NAME`
- all `TERMUX_SERVICE.*` strings
- all `RUN_COMMAND_SERVICE.*` strings

### 4. `app/src/main/res/xml/shortcuts.xml`

Path:

- [shortcuts.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/shortcuts.xml)

This file must be manually patched.

Current hardcoded values:

- `android:targetPackage="com.termux"`
- `android:targetClass="com.termux.app.TermuxActivity"`
- `android:targetClass="com.termux.app.activities.SettingsActivity"`
- extra name `com.termux.app.failsafe_session`

Suggested first-pass edits:

- patch every hardcoded package/class string to `<NEW_PACKAGE>`
- patch the hardcoded extra name if it must stay package-derived

### 5. `scripts/browser-smoke.ps1`

Path:

- [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)

Known hardcoded values:

- `run-as com.termux`
- `/data/user/0/com.termux/files/usr/bin/termux-browser`
- `com.termux/.app.TermuxActivity`

Suggested first-pass edits:

- introduce a single configurable package variable at the top of the script
- derive all:
  - `run-as`
  - private data path
  - activity component name
  from that variable

Why:

- avoids repeated manual edits in every command string
- lets you smoke-test old and new package builds by swapping one variable

## Batch 1 Search-And-Patch Targets

Run targeted searches for:

```text
com.termux
/data/user/0/com.termux
/data/data/com.termux
run-as com.termux
com.termux/.app.TermuxActivity
com.termux.app.TermuxActivity
com.termux.app.activities.SettingsActivity
com.termux.app.failsafe_session
```

## Batch 1 Resource Audit

### Files to inspect, but not necessarily patch immediately

- [root_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/root_preferences.xml)
- [termux_preferences.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/termux_preferences.xml)
- other `app/src/main/res/xml/*.xml`

Current observation:

- many preference XML files reference Java fragment classes under `com.termux.app...`
- these class references are only a blocker if you also rename Java package paths now

First-pass recommendation:

- leave these alone in batch 1 unless installation/runtime tests prove they are blocking

## Batch 1 Things Not To Do Yet

Do not start with:

- moving source tree from `src/main/java/com/termux/**`
- mass-renaming every `package com.termux...`
- editing every document in the repo
- changing plugin modules unless they are part of your immediate ship target

These are valid later tasks, but they create large churn before install/runtime viability is known.

## Expected Batch 1 Outcome

If batch 1 works, you should be able to verify:

1. renamed APK installs alongside official Termux
2. renamed APK launches independently
3. shell starts
4. browser runtime works
5. updated smoke script can target the renamed package

If batch 1 fails, the likely failure areas are:

- `sharedUserId`
- runtime path assumptions in `TermuxConstants`
- bootstrap path coupling
- scripts still pointing at `com.termux`

## Suggested Execution Order

1. Patch [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)
2. Patch [AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)
3. Patch [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
4. Patch [shortcuts.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/shortcuts.xml)
5. Patch [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)
6. Build
7. Install alongside official Termux
8. Run the coexistence test checklist

## Bottom Line

The first pass should target install/runtime identity, not source package cosmetics.

The concrete first-pass patch set is:

- [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)
- [AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)
- [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
- [shortcuts.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/res/xml/shortcuts.xml)
- [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)
