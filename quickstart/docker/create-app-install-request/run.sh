#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

create_app_install_request() {
  local token=$1
  local dsoParty=$2
  local appUserParty=$3
  local appProviderParty=$4
  local participantUserId=$5
  local participant=$6

  # Add a timestamp for a unique command ID to allow resubmission
  local time="$(date +%s%N)"

  echo "create_app_install_request $dsoParty $appUserParty $appProviderParty $participant" >&2

  curl_check "http://$participant/v2/commands/submit-and-wait" "$token" "application/json" \
    --data-raw '{
        "commands": [
          {
            "CreateCommand": {
              "templateId": "#quickstart-licensing:Licensing.AppInstall:AppInstallRequest",
              "createArguments": {
                "dso": "'$dsoParty'",
                "provider": "'$appProviderParty'",
                "user": "'$appUserParty'",
                "meta": {
                  "values": []
                }
              }
            }
          }
        ],
        "workflowId": "create-app-install-request",
        "applicationId": "'$participantUserId'",
        "commandId": "create-app-install-request-'$time'",
        "deduplicationPeriod": {
          "Empty": {}
        },
        "actAs": [
          "'$appUserParty'"
        ],
        "readAs": [
          "'$appUserParty'"
        ],
        "submissionId": "create-app-install-request",
        "disclosedContracts": [],
        "domainId": "",
        "packageIdSelectionPreference": []
    }'
}

if [ "$AUTH_MODE" == "oauth2" ]; then

  APP_USER_WALLET_ADMIN_TOKEN=$(get_user_token $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD $AUTH_APP_USER_AUTO_CONFIG_CLIENT_ID $AUTH_APP_USER_TOKEN_URL)
  DSO_PARTY=$(get_dso_party_id "$APP_USER_WALLET_ADMIN_TOKEN" "splice:2${VALIDATOR_ADMIN_API_PORT_SUFFIX}")

  create_app_install_request "$APP_USER_WALLET_ADMIN_TOKEN" $DSO_PARTY $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_ID "canton:2${PARTICIPANT_JSON_API_PORT_SUFFIX}"

else
  APP_USER_WALLET_ADMIN_TOKEN=$(generate_jwt "$AUTH_APP_USER_WALLET_ADMIN_USER_NAME" "$AUTH_APP_USER_AUDIENCE")
  DSO_PARTY=$(get_dso_party_id "$APP_USER_WALLET_ADMIN_TOKEN" "splice:2${VALIDATOR_ADMIN_API_PORT_SUFFIX}")

  create_app_install_request "$APP_USER_WALLET_ADMIN_TOKEN" $DSO_PARTY $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_WALLET_ADMIN_USER_NAME "canton:2${PARTICIPANT_JSON_API_PORT_SUFFIX}"
fi

