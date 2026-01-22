#!/usr/bin/env bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#
# Stop DevNet services for cn-quickstart
#
# Usage: ./stop.sh [options]
#   -v    Also remove volumes (persistent data)
#   -h    Show this help message

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Default options
REMOVE_VOLUMES=""
SHOW_HELP=false

# Parse command line options
while getopts "vh" opt; do
  case $opt in
    v)
      REMOVE_VOLUMES="-v"
      ;;
    h)
      SHOW_HELP=true
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
  esac
done

if [ "$SHOW_HELP" = true ]; then
  echo "DevNet Services Stop Script"
  echo ""
  echo "Usage: $0 [options]"
  echo ""
  echo "Options:"
  echo "  -v    Also remove volumes (deletes persistent data like databases)"
  echo "  -h    Show this help message"
  echo ""
  exit 0
fi

echo "=== DevNet Services Shutdown ==="

# Change to the devnet directory
cd "${SCRIPT_DIR}"

if [ -n "$REMOVE_VOLUMES" ]; then
  echo "Stopping services and removing volumes..."
else
  echo "Stopping services (preserving volumes)..."
fi

# Stop and remove containers
docker compose --env-file compose.env --profile devnet down ${REMOVE_VOLUMES}

echo ""
echo "DevNet services stopped."
