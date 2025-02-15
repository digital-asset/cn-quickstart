#!/usr/bin/env bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

# fetch onboarding secret from super-validator if none is provided. Only works in LocalNet and DevNet
if [[ -z "${SPLICE_APP_VALIDATOR_ONBOARDING_SECRET:-}" ]]; then
    echo "No onboarding secret provided. Attempting to fetch from super-validator via $ONBOARDING_SECRET_URL..."
    export SPLICE_APP_VALIDATOR_ONBOARDING_SECRET="$(curl -sfL -X POST "$ONBOARDING_SECRET_URL")"
fi

exec /app/entrypoint.sh "$@"
