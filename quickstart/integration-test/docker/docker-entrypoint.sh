#!/usr/bin/env sh
set -e

# Start the Docker daemon (via the standard dind entrypoint) in the background
dockerd-entrypoint.sh &

# Wait until `docker info` succeeds (meaning the daemon is ready)
echo "Waiting for Docker daemon to come up..."
while ! docker info >/dev/null 2>&1; do
    sleep 1
done

echo "Docker daemon is running. Executing: $*"
exec "$@"
