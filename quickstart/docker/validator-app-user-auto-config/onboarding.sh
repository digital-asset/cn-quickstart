#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

#
#get_app_user_admin_token2() {
#  local secret=$1
#  echo "get_app_user_admin_token $secret" >&2
#  if [ -z "$secret" ]; then
#    echo "Secret is empty" >&2
#    exit 1
#  fi
#
##    -H "Host: $AUTH_APP_USER_ISSUER_HOST" \
#  curl -v -s -S "http://nginx-keycloak:8082/realms/AppUser/protocol/openid-connect/token" \
#    -H 'Content-Type: application/x-www-form-urlencoded' \
#    -d 'client_id=app-user-validator' \
#    -d 'client_secret='${secret} \
#    -d 'grant_type=client_credentials' \
#    -d 'scope=openid' | jq -r .access_token
#}
#
#test=$(get_app_user_admin_token2 $AUTH_APP_USER_VALIDATOR_CLIENT_SECRET)
#echo "test: $test"

APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_admin_token $AUTH_APP_USER_VALIDATOR_CLIENT_SECRET)

if [ ! -f /tmp/onboarding-dars-uploaded ]; then
  upload_dars "$APP_USER_PARTICIPANT_ADMIN_TOKEN" participant-app-user
  touch /tmp/onboarding-dars-uploaded
fi

APP_USER_PARTY=$(get_user_party "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID participant-app-user)

# To update user name in metadata
update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_WALLET_ADMIN_USER_ID $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $APP_USER_PARTY participant-app-user
update_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID $AUTH_APP_USER_VALIDATOR_USER_NAME $APP_USER_PARTY participant-app-user

# needed for create-app-install-request
grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_WALLET_ADMIN_USER_ID $APP_USER_PARTY "ReadAs ActAs" participant-app-user

delete_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" participant_admin participant-app-user
