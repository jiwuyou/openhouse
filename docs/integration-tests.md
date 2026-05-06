# Integration Tests

This document tracks cross-project checks that prove the OpenHouse local stack
works as a product.

## Current Smoke Checks

1. service-manager is reachable on `127.0.0.1:8787`.
2. `GET /api/v1/groups` returns `local-stack`.
3. `cc-connect` is running and owns `9810`, `9820`, `9840`.
4. `smallphone-stack` is running and owns `3100`, `18080`, `18082`, `18096`.
5. `file-transfer-18081` is running and owns `18081`.
6. `http://127.0.0.1:18082/` returns the SmallPhone desktop page.
7. `http://127.0.0.1:3100/api/bootstrap` returns SmallPhone bootstrap JSON.

## Recommended Future Checks

- Verify OpenHouse APK can load the online maintenance manifest.
- Verify bootstrap stages are idempotent.
- Verify service-manager can stop and start `group:local-stack`.
- Verify cc-proxy can pass a Claude Code request to the configured upstream.
- Verify docs and guide sites build or serve locally.

