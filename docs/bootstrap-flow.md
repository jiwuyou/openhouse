# Bootstrap Flow

OpenHouse Bootstrap owns installation and first-run setup.

Repository:

```text
/root/openhouse-bootstrap
```

## Intended Flow

1. User installs OpenHouse APK or official Termux.
2. User runs bootstrap or APK maintenance center invokes bootstrap stages.
3. Bootstrap installs Ubuntu through `proot-distro` where needed.
4. Bootstrap installs OpenCode, Codex, Claude Code, and skills.
5. Bootstrap calls child repository install/check entry points for required
   OpenHouse runtime components.
6. Bootstrap registers or starts core services through service-manager.
7. User opens the OpenHouse mobile/web entry point.

## Bootstrap Commands

```bash
bash bootstrap.sh full
bash bootstrap.sh ubuntu
bash bootstrap.sh opencode
bash bootstrap.sh codex
bash bootstrap.sh claude-code
bash bootstrap.sh skills
bash bootstrap.sh required-components
bash bootstrap.sh start
```

## Integration Principle

Bootstrap should orchestrate installation and configuration. Child repositories
own their own install scripts. Long-running process control should belong to
service-manager.

See [Install contract](install-contract.md) for the repository ownership rules.
