#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh
APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_app_provider_admin_token $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET)
APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_admin_token $AUTH_APP_USER_VALIDATOR_CLIENT_SECRET)

if [ ! -f /tmp/onboarding-dars-uploaded ]; then
  upload_dars "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant-app-provider
  upload_dars "$APP_USER_PARTICIPANT_ADMIN_TOKEN" participant-app-user
  touch /tmp/onboarding-dars-uploaded
fi

APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID participant-app-provider)
APP_USER_PARTY=$(get_user_party "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID participant-app-user)

# create users for pqs,backend for app-provider
create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_ID $AUTH_APP_PROVIDER_PQS_USER_NAME "" participant-app-provider
grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PQS_USER_ID $APP_PROVIDER_PARTY "ReadAs" participant-app-provider

create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_BACKEND_USER_ID $AUTH_APP_PROVIDER_BACKEND_USER_NAME "" participant-app-provider
grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_BACKEND_USER_ID $APP_PROVIDER_PARTY "ReadAs ActAs" participant-app-provider

# To update user name in metadata
update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME $APP_PROVIDER_PARTY participant-app-provider
update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_WALLET_ADMIN_USER_ID $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $APP_USER_PARTY participant-app-user

# needed for create-app-install-request
grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_WALLET_ADMIN_USER_ID $APP_USER_PARTY "ReadAs ActAs" participant-app-user

# update user name in metadata
update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME $APP_PROVIDER_PARTY participant-app-provider
update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID $AUTH_APP_USER_VALIDATOR_USER_NAME $APP_USER_PARTY participant-app-user

delete_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant_admin participant-app-provider
delete_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" participant_admin participant-app-user
