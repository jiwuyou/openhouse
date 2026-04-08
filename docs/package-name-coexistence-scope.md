# Package Name Coexistence Scope

## Purpose

This document lists the file scope that needs review when changing the Android package name so this app can coexist with official `Termux`.

Target problem:

- current install identity is still `com.termux`
- as long as that remains true, it will conflict with official Termux
- changing only branding is not enough

This document focuses on:

- minimum files that must change
- files that are strongly coupled and must be audited
- files that can be deferred

## Scope Summary

There are two different changes:

1. Brand/display rename
2. Android package / application identity rename

This project has already done the first one in several places.

For coexistence with official Termux, the second one is required.

That means the main work is not just changing app name strings. It is changing all places coupled to:

- `com.termux`
- `TERMUX_PACKAGE_NAME`
- Android permission names
- content provider authorities
- intent actions
- app-private filesystem paths
- `run-as com.termux`

## Must Change

### 1. App Gradle Identity

File:

- [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)

Why:

- defines `namespace "com.termux"`
- defines `manifestPlaceholders.TERMUX_PACKAGE_NAME = "com.termux"`

This is the first hard stop for coexistence.

At minimum, review and likely change:

- `namespace`
- `manifestPlaceholders.TERMUX_PACKAGE_NAME`
- any future `applicationId` if explicitly added

### 2. Main Manifest Package-Derived Identity

File:

- [app/src/main/AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)

Why:

Many external Android identities are derived from `${TERMUX_PACKAGE_NAME}`:

- `sharedUserId`
- custom permission names
- `provider` authorities
- `RUN_COMMAND` intent action
- `taskAffinity`

These are install/runtime identity items, not cosmetic strings.

Important examples inside this manifest:

- `${TERMUX_PACKAGE_NAME}.permission.RUN_COMMAND`
- `${TERMUX_PACKAGE_NAME}.documents`
- `${TERMUX_PACKAGE_NAME}.files`
- `${TERMUX_PACKAGE_NAME}.RUN_COMMAND`

### 3. Shared Constants Backbone

File:

- [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)

Why:

This file is the central coupling point for package name, paths, permissions, plugin package names, and action names.

It explicitly states:

- `TERMUX_PACKAGE_NAME` must match the app `applicationId`
- changing package name affects paths and compiled bootstrap assumptions

Critical constants to review:

- `TERMUX_PACKAGE_NAME`
- `TERMUX_FILE_SHARE_URI_AUTHORITY`
- app/plugin package names built from `TERMUX_PACKAGE_NAME`
- intent action constants under `RUN_COMMAND_SERVICE`
- file path constants containing `com.termux`

This file is not optional for a coexistence change.

## Must Audit Carefully

### 4. termux-shared Module

Directory:

- `app/termux-app-custom/termux-shared/src/**`

Why:

This module contains the shared runtime contract used by app code, plugins, and external command interfaces.

Expect coupling in:

- package-name checks
- URI authorities
- RUN_COMMAND integration
- file path generation
- plugin package names
- launcher/main activity names

Even if every file does not require edits, the whole module must be searched.

### 5. Scripts And Tooling

Directory:

- `scripts/**`

Confirmed examples:

- [browser-smoke.ps1](/C:/Users/24045/termuweb/scripts/browser-smoke.ps1)

Why:

These scripts directly reference current package identity:

- `run-as com.termux`
- `/data/user/0/com.termux/...`
- `com.termux/.app.TermuxActivity`

If package name changes, these scripts will stop working until updated.

### 6. Any Runtime Paths Hardcoded To `com.termux`

Search targets:

- `/data/user/0/com.termux`
- `/data/data/com.termux`
- `run-as com.termux`
- `com.termux/.app.TermuxActivity`

Why:

Even if Android manifest and Gradle are changed correctly, these path references will still break:

- adb automation
- browser command wrappers
- SSH/Ubuntu helper flows
- docs that users copy from

## Likely Must Change

### 7. Resource XML That Cannot Use Dynamic Placeholder Logic Well

Files to audit:

- `app/src/main/res/xml/shortcuts.xml`
- `app/src/main/res/xml/*preferences*.xml`
- `app/src/main/res/values/strings.xml`
- `termux-shared/src/main/res/**`

Why:

Some XML resources cannot rely on dynamic placeholder substitution the same way as manifest entries.

`TermuxConstants.java` itself warns that these resource files need review when package name changes.

### 8. Tests

Directories:

- `app/src/test/**`
- `termux-shared/src/androidTest/**`
- any Android instrumentation test sources

Why:

Tests often hardcode:

- authorities
- intent actions
- package name assumptions
- storage paths

## High-Risk Design Item

### 9. `sharedUserId`

File:

- [app/src/main/AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)

Current entry:

- `android:sharedUserId="${TERMUX_PACKAGE_NAME}"`

Why this is risky:

- coexistence usually means this app must be treated as a distinct app identity
- keeping a Termux-style shared user strategy may conflict with signing and package identity assumptions
- even if install succeeds, runtime sharing assumptions may not hold as expected

This line must be deliberately re-evaluated, not mechanically renamed.

## Usually Can Be Deferred

### 10. Java/Kotlin Source Package Paths

Examples:

- `app/src/main/java/com/termux/**`
- `termux-shared/src/main/java/com/termux/**`

Why defer:

- changing source package paths is not the first blocker for coexistence
- Android install identity is usually blocked earlier by Gradle/manifest/runtime constants
- path/package refactors are high-churn and can be postponed until install/runtime identity is stable

This means:

- you do not need to start by renaming every `package com.termux...`
- you do need to ensure external Android identity is no longer `com.termux`

## Minimum Practical Change Set

If the goal is only:

- install alongside official Termux

Then the smallest realistic scope is:

1. [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)
2. [app/src/main/AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)
3. [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
4. `scripts/**` references to current package/path identity
5. resource XML and string/entity files noted by `TermuxConstants`

Without those, coexistence work is incomplete.

## Recommended Work Order

1. Change install/runtime identity first.
   Review:
   - Gradle namespace/applicationId path
   - manifest placeholder package name
   - manifest-derived permissions/authorities/actions

2. Update shared constants second.
   Review:
   - package constant
   - file path constants
   - plugin package/action constants

3. Update scripts and command wrappers third.
   Review:
   - adb helpers
   - browser automation
   - SSH/Ubuntu helper docs/scripts

4. Only then decide whether to rename Java package paths.

## Bottom Line

For coexistence with official Termux:

- branding changes are not enough
- a few key files are definitely required
- but a full source tree package rename is not the correct first step

The highest-value files to start with are:

- [app/build.gradle](/C:/Users/24045/termuweb/app/termux-app-custom/app/build.gradle)
- [app/src/main/AndroidManifest.xml](/C:/Users/24045/termuweb/app/termux-app-custom/app/src/main/AndroidManifest.xml)
- [TermuxConstants.java](/C:/Users/24045/termuweb/app/termux-app-custom/termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java)
