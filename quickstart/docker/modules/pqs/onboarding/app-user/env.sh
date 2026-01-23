#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

if [ "$AUTH_MODE" == "oauth2" ]; then
  # create user for pqs for app-user
  create_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PQS_USER_ID $AUTH_APP_USER_PQS_USER_NAME "" "canton:2${PARTICIPANT_JSON_API_PORT_SUFFIX}"
  grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PQS_USER_ID $APP_USER_PARTY "ReadAs" "canton:2${PARTICIPANT_JSON_API_PORT_SUFFIX}"

else
  create_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PQS_USER_NAME $AUTH_APP_USER_PQS_USER_NAME "" "canton:2${PARTICIPANT_JSON_API_PORT_SUFFIX}"
  grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PQS_USER_NAME $APP_USER_PARTY "ReadAs" "canton:2${PARTICIPANT_JSON_API_PORT_SUFFIX}"

  # we need share token
  APP_USER_PQS_USER_TOKEN=$(generate_jwt "$AUTH_APP_USER_PQS_USER_NAME" "$AUTH_APP_USER_AUDIENCE")
  share_file "app-user-pqs.conf" <<EOF
  pipeline.oauth.accessToken="${APP_USER_PQS_USER_TOKEN}"
EOF
fi
