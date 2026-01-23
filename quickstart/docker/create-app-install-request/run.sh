#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# This script is executed by the `splice-onboarding` container. It leverages provided functions from `/app/utils`
# and the resolved environment to initiate Licensing Workflow by creating an App Install Request on behalf of the App User.
# Note: This script is intended for local development environment only and is not meant for production use.

set -eo pipefail

source /app/utils.sh

if [ "$TEST_MODE" == "on" ] && [ -n "$TEST_UNIQUE_REQUEST_TAG" ]; then
  AUTH_APP_USER_WALLET_ADMIN_USER_NAME=$TEST_AUTH_APP_USER_WALLET_ADMIN_USER_NAME
  AUTH_APP_USER_WALLET_ADMIN_USER_ID=$TEST_AUTH_APP_USER_WALLET_ADMIN_USER_ID
  APP_USER_PARTY=$TEST_APP_USER_PARTY
fi

create_app_install_request() {
  local token=$1
  local appUserParty=$2
  local appProviderParty=$3
  local participantUserId=$4
  local participant=$5
  local uniqueTestTag=$6

  local uniqueRequestIdentifier="$(date +%s%N)"
  local metadata="{}"

  if [ -n "${uniqueTestTag}" ]; then
    uniqueRequestIdentifier=${uniqueTestTag}
    metadata="{\"test\":\"$uniqueTestTag\"}"
  fi

  echo "create_app_install_request $appUserParty $appProviderParty $participant $uniqueTestTag" >&2
  local body=$(cat << EOF
      {
        "commands": [
          {
            "CreateCommand": {
              "templateId": "#quickstart-licensing:Licensing.AppInstall:AppInstallRequest",
              "createArguments": {
                "provider": "$appProviderParty",
                "user": "$appUserParty",
                "meta": {
                  "values": $metadata
                }
              }
            }
          }
        ],
        "workflowId": "create-app-install-request",
        "applicationId": "$participantUserId",
        "commandId": "create-app-install-request-$uniqueRequestIdentifier",
        "deduplicationPeriod": {
          "Empty": {}
        },
        "actAs": [
          "$appUserParty"
        ],
        "readAs": [
          "$appUserParty"
        ],
        "submissionId": "create-app-install-request",
        "disclosedContracts": [],
        "domainId": "",
        "packageIdSelectionPreference": []
      }
EOF
)

  curl_check "http://$participant/v2/commands/submit-and-wait" "$token" "application/json" \
    --data-raw "$body"
}

if [ "$AUTH_MODE" == "oauth2" ]; then
  # generate APP_USER_PARTICIPANT_ADMIN_TOKEN on every run
  APP_USER_WALLET_ADMIN_TOKEN=$(get_user_token $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD $AUTH_APP_USER_AUTO_CONFIG_CLIENT_ID $AUTH_APP_USER_TOKEN_URL)
  create_app_install_request "$APP_USER_WALLET_ADMIN_TOKEN" $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_ID "$CANTON_HOST:2${PARTICIPANT_JSON_API_PORT_SUFFIX}" ${TEST_UNIQUE_REQUEST_TAG}

else
  # static APP_USER_WALLET_ADMIN_TOKEN
  create_app_install_request "$APP_USER_WALLET_ADMIN_TOKEN" $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_NAME "$CANTON_HOST:2${PARTICIPANT_JSON_API_PORT_SUFFIX}" "${TEST_UNIQUE_REQUEST_TAG}"
fi

