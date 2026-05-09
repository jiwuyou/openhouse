#!/usr/bin/env sh
set -eu

missing=0

check_required() {
  repo="$1"
  path="$2"
  label="$3"

  if [ -e "$repo/$path" ]; then
    printf 'OK   %-48s %s\n' "$repo" "$label"
  else
    printf 'MISS %-48s %s (%s)\n' "$repo" "$label" "$path"
    missing=1
  fi
}

check_optional() {
  repo="$1"
  path="$2"
  label="$3"

  if [ -e "$repo/$path" ]; then
    printf 'OK   %-48s %s\n' "$repo" "$label"
  else
    printf 'WARN %-48s %s not present (%s)\n' "$repo" "$label" "$path"
  fi
}

printf 'OpenHouse install contract check\n'

check_required /root/termux-src/termux-app app/src/main/assets/maintainer "APK maintenance assets"
check_required /root/openhouse-bootstrap bootstrap.sh "bootstrap orchestrator"

for repo in \
  /root/cc-connect-fresh \
  /root/projects/cc-proxy \
  /root/projects/openhouse-key-tool \
  /root/projects/smallphone/smallphone-active \
  /root/projects/service-manager
do
  check_required "$repo" scripts/install.sh "install entry point"
  check_required "$repo" scripts/check.sh "check entry point"
  check_optional "$repo" scripts/register-service.sh "service registration"
done

check_optional /root/openhouse-app-guide-site scripts/sync.sh "static guide sync"
check_optional /root/openhouse-docs scripts/install.sh "docs build dependency install"
check_optional /root/openhouse-docs scripts/check.sh "docs build check"

exit "$missing"
