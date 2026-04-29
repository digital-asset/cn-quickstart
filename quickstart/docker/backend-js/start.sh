#!/bin/sh
set -eu

# Mirror Java's start.sh: source every onboarding script so APP_PROVIDER_PARTY etc. are exported.
for script in /onboarding/backend-service/on/*.sh; do
  [ -f "$script" ] && . "$script"
done

exec node /app/dist/server.js
