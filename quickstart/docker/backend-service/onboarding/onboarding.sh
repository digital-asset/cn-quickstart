#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

init() {
  local backendUserId=$1
  create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $backendUserId $AUTH_APP_PROVIDER_BACKEND_USER_NAME "" "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
  grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $backendUserId $APP_PROVIDER_PARTY "ReadAs ActAs" "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
}

if [ "$AUTH_MODE" == "oauth2" ]; then
  init "$AUTH_APP_PROVIDER_BACKEND_USER_ID"
  share_file "backend-service/on/backend-service.sh" <<EOF
  export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
EOF

else
  init "$AUTH_APP_PROVIDER_BACKEND_USER_NAME"
  APP_PROVIDER_BACKEND_USER_TOKEN=$(generate_jwt "$AUTH_APP_PROVIDER_BACKEND_USER_NAME" "$AUTH_APP_PROVIDER_AUDIENCE")
  share_file "backend-service/on/backend-service.sh" <<EOF
  export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
  export APP_PROVIDER_BACKEND_USER_TOKEN=${APP_PROVIDER_BACKEND_USER_TOKEN}
EOF
fi