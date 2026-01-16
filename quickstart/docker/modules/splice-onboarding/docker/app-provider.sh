#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

if [ "$AUTH_MODE" = "oauth2" ]; then
  export APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_admin_token $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_ID $AUTH_APP_PROVIDER_TOKEN_URL)
  export APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}")

  if [ "$DO_INIT" == "true" ] && [ ! -f /tmp/app-provider-init-user-cleanup ]; then
    # To update username in metadata
    update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME $APP_PROVIDER_PARTY "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
    update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME $APP_PROVIDER_PARTY "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
    delete_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant_admin "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
    touch /tmp/app-provider-init-user-cleanup

  fi

else
  export APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(generate_jwt "$AUTH_APP_PROVIDER_VALIDATOR_USER_NAME" "$AUTH_APP_PROVIDER_AUDIENCE")
  export APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}")

  if [ "$DO_INIT" == "true" ] && [ ! -f /tmp/app-provider-init-user-cleanup ]; then
    delete_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant_admin "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
    touch /tmp/app-provider-init-user-cleanup

  fi
fi
