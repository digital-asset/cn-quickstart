#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_admin_token $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_ID $AUTH_APP_PROVIDER_TOKEN_URL)

if [ ! -f /tmp/onboarding-dars-uploaded ]; then
  upload_dars "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant-app-provider
  touch /tmp/onboarding-dars-uploaded
fi

APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID participant-app-provider)

# create users for pqs,backend for app-provider
create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_ID $AUTH_APP_PROVIDER_PQS_USER_NAME "" participant-app-provider
grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_ID $APP_PROVIDER_PARTY "ReadAs" participant-app-provider

create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_BACKEND_USER_ID $AUTH_APP_PROVIDER_BACKEND_USER_NAME "" participant-app-provider
grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_BACKEND_USER_ID $APP_PROVIDER_PARTY "ReadAs ActAs" participant-app-provider

# To update username in metadata
update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME $APP_PROVIDER_PARTY participant-app-provider
update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME $APP_PROVIDER_PARTY participant-app-provider
delete_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant_admin participant-app-provider
