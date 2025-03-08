#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
APP_PROVIDER_PARTY=$1

source /app/utils.sh

create_app_install_request() {
  local token=$1
  local dsoParty=$2
  local appUserParty=$3
  local appProviderParty=$4
  local participant=$5
  local time="$(date +%s)"
  curl_check "http://$participant:7575/v2/commands/submit-and-wait" "$token" "application/json" \
    --data-raw '{
        "commands" : [
           { "CreateCommand" : {
                "template_id": "#quickstart-licensing:Licensing.AppInstall:AppInstallRequest",
                "create_arguments": {
                    "dso": "'$dsoParty'",
                    "provider": "'$appProviderParty'",
                    "user": "'$appUserParty'",
                    "meta": {"values": []}
                }
            }
           }

        ],
        "workflow_id" : "create-app-install-request",
        "application_id": "ledger-api-user",
        "command_id": "create-app-install-request-'$time'",
        "deduplication_period": { "Empty": {} },
        "act_as": ["'$appUserParty'"],
        "read_as": ["'$appUserParty'"],
        "submission_id": "create-app-install-request",
        "disclosed_contracts": [],
        "domain_id": "",
        "package_id_selection_preference": []
    }'
}

LEDGER_API_ADMIN_USER_TOKEN_APP_USER=$(get_token $LEDGER_API_ADMIN_USER $AUTH_APP_USER_CLIENT_ID)
APP_USER_PARTY=$(get_user_party "$LEDGER_API_ADMIN_USER_TOKEN_APP_USER" $AUTH_APP_USER_CLIENT_ID participant-app-user)
WALLET_ADMIN_USER_TOKEN_APP_USER=$(get_token $WALLET_ADMIN_USER $AUTH_APP_USER_CLIENT_ID)
DSO_PARTY=$(get_dso_party_id "$WALLET_ADMIN_USER_TOKEN_APP_USER" validator-app-user)

create_app_install_request "$LEDGER_API_ADMIN_USER_TOKEN_APP_USER" $DSO_PARTY $APP_USER_PARTY $APP_PROVIDER_PARTY participant-app-user


