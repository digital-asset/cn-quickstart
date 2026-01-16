#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

AUTH_SV_VALIDATOR_USER_NAME=ledger-api-user

 # we need share token
SV_PQS_USER_TOKEN=$(generate_jwt "$AUTH_SV_VALIDATOR_USER_NAME" "$AUTH_APP_PROVIDER_AUDIENCE")
share_file "sv-pqs.conf" <<EOF
pipeline.oauth.accessToken="${SV_PQS_USER_TOKEN}"
EOF

