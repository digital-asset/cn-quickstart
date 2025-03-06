#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh
APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_app_provider_admin_token $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET)
APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_admin_token $AUTH_APP_USER_VALIDATOR_CLIENT_SECRET)
APP_USER_PARTY=$(get_user_party "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID participant-app-user)


register_tenant() {
  local token=$1
  local partyId=$2
  local clientId=$3
  local issuerUrl=$4
  echo "register_tenant $token $partyId $clientId $issuerUrl" >&2
  curl_check "http://backend-service:8080/admin/tenant-registrations" "$token" "application/json" \
   --data-raw '{
     "tenantId": "AppUser",
     "partyId": "'$partyId'",
     "walletUrl": "http://wallet.localhost:2000/",
     "clientId": "'$clientId'",
     "issuerUrl": "'$issuerUrl'",
     "internal": false
   }'
}

register_tenant "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" "$APP_USER_PARTY" "$AUTH_APP_USER_BACKEND_OIDC_CLIENT_ID" "$AUTH_APP_USER_ISSUER_URL"