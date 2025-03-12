#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_admin_token $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_ID $AUTH_APP_PROVIDER_TOKEN_URL)
export AUTH_APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID participant-app-provider)

tar -xf /backend.tar -C /opt
/opt/backend/bin/backend