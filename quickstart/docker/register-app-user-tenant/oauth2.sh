#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# This script is executed by the `splice-onboarding` container. It leverages provided functions from `/app/utils`
# and the resolved environment to register App User tenant to the backend service.
# Note: This script is intended for local development environment only and is not meant for production use.

set -eo pipefail

source /app/utils.sh

register_tenant() {
  local token=$1
  local partyId=$2
  local clientId=$3
  local issuerUrl=$4
  echo "register_tenant $partyId $clientId $issuerUrl" >&2
  curl_check "http://backend-service:${BACKEND_PORT}/admin/tenant-registrations" "$token" "application/json" \
   --data-raw '{
     "tenantId": "AppUser",
     "partyId": "'$partyId'",
     "walletUrl": "http://wallet.localhost:'${APP_USER_UI_PORT}'",
     "clientId": "'$clientId'",
     "issuerUrl": "'$issuerUrl'",
     "internal": false
   }'
}

register_tenant "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" "$APP_USER_PARTY" "$AUTH_APP_USER_BACKEND_OIDC_CLIENT_ID" "$AUTH_APP_USER_ISSUER_URL"
