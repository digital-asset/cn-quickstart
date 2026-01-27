#!/usr/bin/env bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0
#
# Start DevNet services for cn-quickstart
# This script runs Docker Compose from the devnet directory to avoid auto-merging
# with the root compose.yaml which contains LocalNet service definitions.
#
# Usage: ./start.sh [options]
#   -d    Run in detached mode (background)
#   -h    Show this help message
#
# Note: DevNet always uses OAuth2 authentication (required by splice-node validator)
#
# Prerequisites:
#   1. splice-node validator connected to DevNet (with -a flag for OAuth2)
#   2. Backend built: make build-backend (from quickstart directory)
#   3. Frontend built: make build-frontend (from quickstart directory)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
QUICKSTART_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

# Default options
DETACH=""
SHOW_HELP=false

# Parse command line options
while getopts "dh" opt; do
  case $opt in
    d)
      DETACH="-d"
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
  echo "DevNet Services Startup Script"
  echo ""
  echo "Usage: $0 [options]"
  echo ""
  echo "Options:"
  echo "  -d    Run in detached mode (background)"
  echo "  -h    Show this help message"
  echo ""
  echo "Note: DevNet always uses OAuth2 authentication (required by splice-node validator)"
  echo ""
  exit 0
fi

echo "=== DevNet Services Startup ==="
echo "Quickstart Dir: ${QUICKSTART_DIR}"
echo ""

# Check that backend is built
if [ ! -f "${QUICKSTART_DIR}/backend/build/distributions/backend.tar" ]; then
  echo "ERROR: Backend not built. Please run 'make build-backend' first."
  exit 1
fi

# Check that keycloak config exists (always required for DevNet)
if [ ! -d "${SCRIPT_DIR}/conf/keycloak" ]; then
  echo "ERROR: Keycloak configuration not found at ${SCRIPT_DIR}/conf/keycloak"
  echo "Please ensure realm configuration files are in this directory."
  exit 1
fi

# Change to the devnet directory to avoid auto-merging with root compose.yaml
cd "${SCRIPT_DIR}"

# Load environment variables from compose.env if it exists
if [ -f "compose.env" ]; then
  echo "Loading environment from compose.env"
  set -a
  source compose.env
  set +a
fi

echo ""
echo "Starting DevNet services (OAuth2 authentication)..."
echo "Services: postgres-keycloak, keycloak, nginx-keycloak, postgres-pqs, pqs-app-provider, splice-onboarding, backend-service"
echo ""

# Run docker compose with the devnet profile from the devnet directory to avoid conflicts in compose.yaml
docker compose --env-file compose.env --profile devnet up ${DETACH}
