#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

init() {
  local backendUserId=$1
  local walletUserId=$2

  create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $backendUserId $AUTH_APP_PROVIDER_BACKEND_USER_NAME "" "canton:3${PARTICIPANT_JSON_API_PORT}"
  grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $backendUserId $APP_PROVIDER_PARTY "ReadAs ActAs" "canton:3${PARTICIPANT_JSON_API_PORT}"

  # needed for create-app-install-request
  grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $walletUserId $APP_USER_PARTY "ReadAs ActAs" "canton:2${PARTICIPANT_JSON_API_PORT}"
}


if [ "$AUTH_MODE" == "oauth2" ]; then
  init "$AUTH_APP_PROVIDER_BACKEND_USER_ID" "$AUTH_APP_USER_WALLET_ADMIN_USER_ID"

  share_file "backend-service/on/backend-service.sh" <<EOF
  export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
EOF

else
  init "$AUTH_APP_PROVIDER_BACKEND_USER_NAME" "$AUTH_APP_USER_WALLET_ADMIN_USER_NAME"

  APP_PROVIDER_BACKEND_USER_TOKEN=$(generate_jwt "$AUTH_APP_PROVIDER_BACKEND_USER_NAME" "$AUTH_APP_PROVIDER_AUDIENCE")

  share_file "backend-service/on/backend-service.sh" <<EOF
  export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
  export APP_PROVIDER_BACKEND_USER_TOKEN=${APP_PROVIDER_BACKEND_USER_TOKEN}
EOF

fi