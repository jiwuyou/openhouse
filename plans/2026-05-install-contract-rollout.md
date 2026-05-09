# 2026-05 Install Contract Rollout

## Goal

Move OpenHouse installation ownership into each child repository, then let the
APK maintenance center and `openhouse-bootstrap` orchestrate those install
entry points.

## Decisions

- Each child repository owns its own `scripts/install.sh` and `scripts/check.sh`
  when it has runtime installation work.
- Repositories that run long-lived services also own
  `scripts/register-service.sh`.
- `openhouse-bootstrap` should call those scripts instead of duplicating the
  component-specific install details.
- APK maintenance actions should call bootstrap stages or manifest actions, not
  embed every child repository's internal install logic.
- Developer/source-build toolchains are separate from the default phone runtime
  install.

## Rollout Order

1. `service-manager`
   - Add `scripts/check.sh`.
   - Decide whether service-manager should self-register or only install its
     binary and token/config.

2. `cc-connect-fresh`
   - Add install/check scripts for the mobile bridge runtime.
   - Add service-manager registration for bridge, management, and webclient
     ports.

3. `cc-proxy`
   - Add install/check scripts that prefer npm or release binary install.
   - Keep Rust source builds as an explicit developer mode.
   - Add service-manager registration when it is part of the default stack.

4. `openhouse-key-tool`
   - Add Python CLI install/check scripts.
   - Keep profile examples and smoke checks under the owning repository.

5. `smallphone-active`
   - Add dependency install/check scripts for `smallphone-app` and bundled
     standalone apps.
   - Add service-manager registration for core, beta frontend, and app
     services.

6. `openhouse-app-guide-site`
   - Add a static sync or publish script if it needs to be deployed into the
     phone runtime.

7. `openhouse-docs`
   - Add docs build dependency install/check scripts for build or publish
     workflows only.

8. `openhouse-bootstrap`
   - Replace duplicated component install logic with calls into child
     repository install/check scripts.
   - Keep OpenCode/Codex/Claude Code bootstrap stages until they have a clear
     owning repository or remain intentionally bootstrap-owned.

9. `termux-app`
   - Expose the resulting bootstrap stages in the APK maintenance center.
   - Keep APK-side logic limited to UI, local assets, stage execution, logs, and
     manifest support.

## Verification

Run:

```bash
./scripts/check-install-contract.sh
```

The check should reach zero missing required install/check entry points before
the APK maintenance center presents the new "install required components"
workflow as complete.

