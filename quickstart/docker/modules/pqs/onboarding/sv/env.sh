#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

#if [ "$AUTH_MODE" == "oauth2" ]; then
#  # create user for pqs for app-provider
#  create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_ID $AUTH_APP_PROVIDER_PQS_USER_NAME "" "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
#  grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_ID $APP_PROVIDER_PARTY "ReadAs" "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
#
#else
#  create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_NAME $AUTH_APP_PROVIDER_PQS_USER_NAME "" "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
#  grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_NAME $APP_PROVIDER_PARTY "ReadAs" "canton:3${PARTICIPANT_JSON_API_PORT_SUFFIX}"
#
  # we need share token
  SV_PQS_USER_TOKEN=$(generate_jwt "$AUTH_SV_VALIDATOR_USER_NAME" "$AUTH_APP_PROVIDER_AUDIENCE")
  share_file "sv-pqs.conf" <<EOF
  pipeline.oauth.accessToken="${SV_PQS_USER_TOKEN}"
EOF
#fi

