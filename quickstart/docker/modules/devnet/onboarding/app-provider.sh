#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD
#
# DevNet adaptation: Uses external participant endpoint instead of local canton container
# Gets party from validator API since splice-node manages its own users

set -eo pipefail

source /app/utils.sh

# Get validator party from validator API
get_validator_party() {
  local token=$1
  local validator_host=$2
  echo "get_validator_party from $validator_host" >&2
  
  # The validator API returns the party ID at /api/validator/v0/admin/party
  curl -sf "http://${validator_host}/api/validator/v0/admin/party" \
    -H "Authorization: Bearer $token" \
    -H "Host: validator.localhost" | jq -r '.party_id'
}

# DevNet only supports oauth2 mode
if [ "$AUTH_MODE" = "oauth2" ]; then
  # Get admin token from local Keycloak
  export APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_admin_token $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET $AUTH_APP_PROVIDER_VALIDATOR_CLIENT_ID $AUTH_APP_PROVIDER_TOKEN_URL)
  
  # Get party from validator API (splice-node manages its own users/parties)
  export APP_PROVIDER_PARTY=$(get_validator_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" "${PARTICIPANT_HOST}:${PARTICIPANT_PORT}")

  # If that fails, try getting participant ID and constructing party
  if [ -z "$APP_PROVIDER_PARTY" ] || [ "$APP_PROVIDER_PARTY" = "null" ]; then
    echo "Trying to get party from participant namespace..." >&2
    # Get participant ID and use it as party hint
    PARTICIPANT_ID=$(curl -sf "http://${PARTICIPANT_HOST}:${PARTICIPANT_PORT}/v2/parties/participant-id" \
      -H "Authorization: Bearer $APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" \
      -H "Host: json-ledger-api.localhost" | jq -r '.participantId' | sed 's/^participant:://')
    echo "Participant ID: $PARTICIPANT_ID" >&2
    
    # List parties to find the validator's party
    PARTIES=$(curl -sf "http://${PARTICIPANT_HOST}:${PARTICIPANT_PORT}/v2/parties" \
      -H "Authorization: Bearer $APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" \
      -H "Host: json-ledger-api.localhost")
    echo "Available parties: $PARTIES" >&2
    
    # Take the first party that matches the participant namespace
    export APP_PROVIDER_PARTY=$(echo "$PARTIES" | jq -r ".partyDetails[0].party // empty")
  fi

  echo "DevNet mode: Using external participant at ${PARTICIPANT_HOST}:${PARTICIPANT_PORT}"
  echo "APP_PROVIDER_PARTY resolved to: ${APP_PROVIDER_PARTY}"

  if [ -z "$APP_PROVIDER_PARTY" ] || [ "$APP_PROVIDER_PARTY" = "null" ]; then
    echo "ERROR: Could not resolve APP_PROVIDER_PARTY" >&2
    exit 1
  fi

  # Write shared file for backend-service to source during startup
  # This exports APP_PROVIDER_PARTY env var to the backend-service container
  share_file "backend-service/on/backend-service.sh" <<EOF
export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
EOF

  echo "Created shared file for backend-service with APP_PROVIDER_PARTY"

else
  echo "ERROR: DevNet only supports oauth2 mode" >&2
  exit 1
fi
