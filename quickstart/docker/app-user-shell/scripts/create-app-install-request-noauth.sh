#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
source /app/utils.sh

APP_PROVIDER_PARTY=$(get_user_party "" $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME "canton:3${PARTICIPANT_JSON_API_PORT}")
APP_USER_PARTY=$(get_user_party "" $AUTH_APP_USER_VALIDATOR_USER_NAME "canton:2${PARTICIPANT_JSON_API_PORT}")
APP_USER_WALLET_ADMIN_TOKEN=$(jwt-cli encode hs256 --s unsafe --p '{"sub": "'$AUTH_APP_USER_WALLET_ADMIN_USER_NAME'", "aud": "'$AUTH_APP_USER_AUDIENCE'"}')
DSO_PARTY=$(get_dso_party_id "$APP_USER_WALLET_ADMIN_TOKEN" "splice:2${VALIDATOR_ADMIN_API_PORT}")

#! using $AUTH_APP_USER_WALLET_ADMIN_USER_NAME instead ID
create_app_install_request "" $DSO_PARTY $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_NAME "canton:2${PARTICIPANT_JSON_API_PORT}"
