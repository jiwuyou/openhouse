# termux-packages Bootstrap Rebuild Plan

## Purpose

This document explains the next required step after package coexistence was partially validated in `termux-app-custom`.

Current status:

- app install identity was changed successfully
- `com.openhouse.app` can coexist with official `com.termux`
- browser bridge and smoke tests pass
- full terminal runtime is still blocked by old bootstrap prefix references

So the next required step is:

- rebuild bootstrap assets from `termux-packages` for the new app package / prefix

## What Was Confirmed

### 1. The current bootstrap archives still embed the old prefix

Confirmed examples from installed files and bootstrap content:

- `pkg` shebang originally pointed to:
  - `/data/data/com.termux/files/usr/bin/bash`
- `etc/profile` referenced:
  - `/data/data/com.termux/files/usr/etc/...`
- `etc/bash.bashrc` referenced:
  - `/data/data/com.termux/files/usr/...`
- `apt` / `dpkg` still tried to access:
  - `/data/data/com.termux/files/usr/etc/apt/...`
  - `/data/data/com.termux/files/usr/etc/dpkg/...`

This means the problem is below app-layer config.

### 2. termux-app-side patching is not enough

App-layer changes already succeeded for:

- `applicationId`
- manifest authorities
- `RUN_COMMAND` permission/action
- browser command bridge
- coexistence install

But package management and full shell runtime are still tied to bootstrap contents.

### 3. termux-packages already exposes the correct knobs

In:

- [upstream/termux-packages/scripts/properties.sh](/C:/Users/24045/termuweb/upstream/termux-packages/scripts/properties.sh)

we confirmed the build system explicitly supports changing:

- `TERMUX_APP__PACKAGE_NAME`
- `TERMUX_APP__DATA_DIR`
- `TERMUX__ROOTFS`
- `TERMUX__PREFIX`
- `TERMUX_APP__APP_IDENTIFIER`
- `TERMUX_APP__NAMESPACE`

The file itself states these are the safe variables to modify for a fork.

### 4. There is already a dedicated bootstrap build entrypoint

In:

- [build-bootstraps.sh](/C:/Users/24045/termuweb/upstream/termux-packages/scripts/build-bootstraps.sh)

the script explicitly says it is meant to build bootstrap archives for forked termux apps from local package sources.

That is the correct path forward.

## Key Files In termux-packages

### A. `scripts/properties.sh`

Path:

- [properties.sh](/C:/Users/24045/termuweb/upstream/termux-packages/scripts/properties.sh)

Important variables already identified:

- `TERMUX_APP__PACKAGE_NAME="com.termux"`
- `TERMUX_APP__DATA_DIR="/data/data/$TERMUX_APP__PACKAGE_NAME"`
- `TERMUX__ROOTFS="$TERMUX_APP__DATA_DIR/$TERMUX__ROOTFS_SUBDIR"`
- `TERMUX__PREFIX_SUBDIR="usr"`
- `TERMUX__PREFIX`
- `TERMUX_APP__APP_IDENTIFIER="termux"`
- `TERMUX_APP__NAMESPACE="com.termux"`
- package-derived service/activity class constants

These are the main fork knobs.

### B. `scripts/build-bootstraps.sh`

Path:

- [build-bootstraps.sh](/C:/Users/24045/termuweb/upstream/termux-packages/scripts/build-bootstraps.sh)

Why it matters:

- builds bootstrap archives from local package sources
- designed for forked termux apps

### C. `scripts/generate-bootstraps.sh`

Path:

- [generate-bootstraps.sh](/C:/Users/24045/termuweb/upstream/termux-packages/scripts/generate-bootstraps.sh)

Why it matters:

- fetches packages from published repositories
- useful for understanding the stock bootstrap pipeline
- but for a forked package/prefix, `build-bootstraps.sh` is more relevant

### D. `scripts/bootstrap/*`

Paths include:

- `scripts/bootstrap/termux-bootstrap-second-stage.sh`
- `scripts/bootstrap/01-termux-bootstrap-second-stage-fallback.sh`

Why they matter:

- second-stage install logic is templated using `@TERMUX_PREFIX@`
- these files will naturally adapt if `TERMUX__PREFIX` is changed correctly in `termux-packages`

This is a strong signal that rebuild is the intended fix.

## Recommended Fork Values

If the target app package remains:

```text
com.openhouse.app
```

then the likely first-pass values in `termux-packages/scripts/properties.sh` should be:

- `TERMUX_APP__PACKAGE_NAME="com.openhouse.app"`
- `TERMUX_APP__DATA_DIR="/data/data/com.openhouse.app"`
- `TERMUX_APP__NAMESPACE="com.openhouse.app"`

And then review:

- `TERMUX_APP__APP_IDENTIFIER`

Recommended first-pass identifier:

```text
openhouse
```

Reason:

- avoids reusing `termux` under `TERMUX__APPS_DIR_BY_IDENTIFIER`
- keeps app-specific socket/app-info subpaths separate

## Important Architectural Choice

There are two different identities:

1. Android install identity
2. Java source package identity

For bootstrap rebuild, what matters first is:

- Android/package manager/runtime identity

Not necessarily:

- renaming all Java source packages

That means:

- `TERMUX_APP__PACKAGE_NAME` and path variables must be changed now
- full Java package tree rename can still be deferred

## Practical Next Steps

### Step 1. Create a fork-specific properties overlay

Do not edit upstream defaults blindly first.

Instead:

- create a tracked patch or fork branch
- document changed values

Minimum values to change:

- `TERMUX_APP__PACKAGE_NAME`
- `TERMUX_APP__DATA_DIR`
- `TERMUX_APP__APP_IDENTIFIER`
- `TERMUX_APP__NAMESPACE`

### Step 2. Build local packages for the target architecture

Use `termux-packages` local package build flow to generate packages for:

- `aarch64`

Reason:

- your current device validation is arm64

### Step 3. Run `scripts/build-bootstraps.sh`

Generate a new:

- `bootstrap-aarch64.zip`

and likely all four architectures later:

- `bootstrap-aarch64.zip`
- `bootstrap-arm.zip`
- `bootstrap-i686.zip`
- `bootstrap-x86_64.zip`

### Step 4. Replace app bootstrap assets

Replace in:

- `app/termux-app-custom/app/src/main/assets/`

the generated bootstrap zips used by the app build.

### Step 5. Rebuild APK and retest

Rerun:

- coexistence install test
- `browser-smoke.ps1`
- shell startup
- `pkg`
- `apt`
- `sshd`

## Expected Success Criteria

A successful rebuilt bootstrap should fix:

- `pkg` shebang using old package path
- `etc/profile` old prefix references
- `etc/bash.bashrc` old prefix references
- `apt` / `dpkg` config paths pointing at `com.termux`

And should allow:

- package manager usage under `com.openhouse.app`
- proper shell startup under new prefix
- installation of `openssh`
- independent SSH validation

## What Not To Waste Time On

Until bootstrap is rebuilt, avoid spending time on:

- more app-layer UI tweaks
- more smoke-script renames
- deeper browser tests
- plugin-level validation

Those are no longer the primary blocker.

## Bottom Line

The correct next technical move is no longer inside `termux-app-custom` alone.

It is:

1. patch `upstream/termux-packages/scripts/properties.sh`
2. build new bootstrap archives with the new package/prefix
3. feed those archives back into `termux-app-custom`
4. rerun runtime validation
