#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

export APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_admin_token $AUTH_APP_USER_VALIDATOR_CLIENT_SECRET $AUTH_APP_USER_VALIDATOR_CLIENT_ID $AUTH_APP_USER_TOKEN_URL)
export APP_USER_PARTY=$(get_user_party "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID "canton:2${PARTICIPANT_JSON_API_PORT}")

if [ ! -f /tmp/app-user-onboarding-user-cleanup ]; then
  # To update user name in metadata
  update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_WALLET_ADMIN_USER_ID $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $APP_USER_PARTY "canton:2${PARTICIPANT_JSON_API_PORT}"
  update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID $AUTH_APP_USER_VALIDATOR_USER_NAME $APP_USER_PARTY "canton:2${PARTICIPANT_JSON_API_PORT}"

  delete_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" participant_admin "canton:2${PARTICIPANT_JSON_API_PORT}"
  touch /tmp/app-user-onboarding-user-cleanup
fi
