# OpenHouse Workspace Guidelines

This repository is the top-level coordination repo for OpenHouse.

Do not copy source code from child repositories into this repository. Use this
repository for architecture docs, integration plans, port maps, release notes,
and cross-repository task tracking.

## Child Repositories

- `/root/termux-src/termux-app`
- `/root/openhouse-bootstrap`
- `/root/cc-connect-fresh`
- `/root/projects/cc-proxy`
- `/root/projects/openhouse-key-tool`
- `/root/projects/smallphone/smallphone-active`
- `/root/projects/service-manager`
- `/root/openhouse-app-guide-site`
- `/root/openhouse-docs`

## Working Rules

- Make implementation changes in the child repository that owns the code.
- Keep this repository focused on coordination and documentation.
- Keep child repository install logic in the owning child repository.
- Let `openhouse-bootstrap` and the APK maintenance center orchestrate install
  entry points instead of copying child install internals.
- Prefer each child repository's own build and test commands.
- When changing multiple repositories, write or update a plan under `plans/`.
- Keep secrets, tokens, API keys, and local credentials out of this repository.
- Use `service-manager` as the local runtime control plane for integration work.

## Service Group Convention

Services that belong to a group should include a tag with this format:

```text
group:<name>
```

Example:

```text
group:local-stack
```
