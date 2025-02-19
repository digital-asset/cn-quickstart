#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_app_provider_user_token $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_PASSWORD)
APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_user_token $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_PASSWORD)

export AUTH_APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID participant-app-provider)
export AUTH_APP_USER_PARTY=$(get_user_party "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID participant-app-user)

tar -xf /backend.tar -C /opt
/opt/backend/bin/backend