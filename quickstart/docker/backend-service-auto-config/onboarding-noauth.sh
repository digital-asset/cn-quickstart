#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh
APP_USER_PARTY=$(get_user_party "" $AUTH_APP_USER_VALIDATOR_USER_NAME "canton:2${PARTICIPANT_JSON_API_PORT}")

register_tenant() {
  local providerAdmin=$1
  local partyId=$2
  local tenantUser=$3
  echo "register_tenant $providerAdmin $partyId $tenantUser" >&2

  curl -c cookies.txt -X POST \
    -d "username=${providerAdmin}" \
    http://backend-service:8080/login

  curl_check "http://backend-service:8080/admin/tenant-registrations" "" "application/json" \
   -b cookies.txt \
   -H 'Authorization: Custom' \
   --data-raw '{
     "tenantId": "AppUser",
     "partyId": "'$partyId'",
     "walletUrl": "http://wallet.localhost:2000/",
     "clientId": "",
     "issuerUrl": "",
     "internal": false,
     "users": ["'$tenantUser'"]
   }'
}

register_tenant $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME $APP_USER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_NAME