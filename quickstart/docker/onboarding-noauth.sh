#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -exo pipefail

source /app/utils.sh


#create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_BACKEND_USER_ID $AUTH_APP_PROVIDER_BACKEND_USER_NAME "" "canton:3${PARTICIPANT_JSON_API_PORT}"
#grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_BACKEND_USER_ID $APP_PROVIDER_PARTY "ReadAs ActAs" "canton:3${PARTICIPANT_JSON_API_PORT}"
#
#
## needed for create-app-install-request
#grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_WALLET_ADMIN_USER_ID $APP_USER_PARTY "ReadAs ActAs" "canton:2${PARTICIPANT_JSON_API_PORT}"
