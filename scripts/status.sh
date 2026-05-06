#!/usr/bin/env sh
set -eu

token="${SERVICE_MANAGER_TOKEN:-}"
base="${SERVICE_MANAGER_URL:-http://127.0.0.1:8787}"

printf 'OpenHouse status\n'
printf 'service-manager: %s\n' "$base"

if [ -z "$token" ]; then
  printf 'SERVICE_MANAGER_TOKEN is not set; skipping authenticated group query.\n'
  exit 0
fi

curl -fsS -H "Authorization: Bearer $token" "$base/api/v1/groups"
printf '\n'

