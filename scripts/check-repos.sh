#!/usr/bin/env sh
set -eu

repos="
/root/termux-src/termux-app
/root/openhouse-bootstrap
/root/cc-connect-fresh
/root/projects/cc-proxy
/root/projects/openhouse-key-tool
/root/projects/smallphone/smallphone-active
/root/projects/service-manager
/root/openhouse-app-guide-site
/root/openhouse-docs
"

for repo in $repos; do
  if [ -d "$repo/.git" ]; then
    printf 'OK   %s\n' "$repo"
  elif [ -d "$repo" ]; then
    printf 'WARN %s exists but is not a git repository\n' "$repo"
  else
    printf 'MISS %s\n' "$repo"
  fi
done
