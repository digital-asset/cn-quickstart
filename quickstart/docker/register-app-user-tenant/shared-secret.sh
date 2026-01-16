#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# This script is executed by the `splice-onboarding` container. It leverages provided functions from `/app/utils`
# and the resolved environment to register App User tenant to the backend service.
# Note: This script is intended for local development environment only and is not meant for production use.

set -eo pipefail

source /app/utils.sh

register_tenant() {
  local providerAdmin=$1
  local partyId=$2
  local tenantUser=$3
  echo "register_tenant $providerAdmin $partyId $tenantUser" >&2

  curl -c cookies.txt -X POST \
    -d "username=${providerAdmin}" \
    "http://backend-service:${BACKEND_PORT}/login"

  curl_check "http://backend-service:${BACKEND_PORT}/admin/tenant-registrations" "" "application/json" \
   -b cookies.txt \
   -H 'Authorization: Custom' \
   --data-raw '{
     "tenantId": "AppUser",
     "partyId": "'$partyId'",
     "walletUrl": "http://wallet.localhost:'${APP_USER_UI_PORT}'",
     "clientId": "",
     "issuerUrl": "",
     "internal": false,
     "users": ["'$tenantUser'"]
   }'
}

register_tenant $AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME $APP_USER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_NAME
