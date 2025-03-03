#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh
APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_app_provider_user_token $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_PASSWORD)
APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_user_token $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_PASSWORD)

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
grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_ID $APP_USER_PARTY "ReadAs ActAs" participant-app-user

# onboard users to wallets
# Update PARTICIPANT_ADMIN_USER_ID to add primary party and desc as in participant-onboarding.sh we didn't have party yet and participant created
# PARTICIPANT_ADMIN_USER_ID as participantAdmin without primaryParty.
# To make PROVIDER_PARTICIPANT_ADMIN_USER_ID wallet user he must have primary party assigned.
update_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_ID $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_NAME $APP_PROVIDER_PARTY participant-app-provider
update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_ID $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_NAME $APP_USER_PARTY participant-app-user

APP_PROVIDER_WALLET_ADMIN_USER_TOKEN=$(get_app_provider_user_token $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_PASSWORD)
APP_USER_WALLET_ADMIN_USER_TOKEN=$(get_app_user_user_token $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD)

onboard_wallet_user "$APP_PROVIDER_WALLET_ADMIN_USER_TOKEN" $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_ID $APP_PROVIDER_PARTY validator-app-provider
onboard_wallet_user "$APP_USER_WALLET_ADMIN_USER_TOKEN" $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_ID $APP_USER_PARTY validator-app-user

