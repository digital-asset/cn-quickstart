#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

if [ "$AUTH_MODE" == "oauth2" ]; then
  # create user for pqs for app-user
  create_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PQS_USER_ID $AUTH_APP_USER_PQS_USER_NAME "" "canton:2${PARTICIPANT_JSON_API_PORT}"
  grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PQS_USER_ID $APP_USER_PARTY "ReadAs" "canton:2${PARTICIPANT_JSON_API_PORT}"
fi