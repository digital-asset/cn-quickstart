#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
APP_PROVIDER_PARTY=$1
APP_USER_PARTY=$2
DSO_PARTY=$3
APP_USER_PARTICIPANT_ADMIN_TOKEN=$4

source /app/utils.sh

create_app_install_request() {
  local token=$1
  local dsoParty=$2
  local appUserParty=$3
  local appProviderParty=$4
  local participantAdminUserId=$5
  local participant=$6

  echo "create_app_install_request $dsoParty $appUserParty $appProviderParty $participant" >&2

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
        "application_id": "'$participantAdminUserId'",
        "command_id": "create-app-install-request",
        "deduplication_period": { "Empty": {} },
        "act_as": ["'$appUserParty'"],
        "read_as": ["'$appUserParty'"],
        "submission_id": "create-app-install-request",
        "disclosed_contracts": [],
        "domain_id": "",
        "package_id_selection_preference": []
    }'
}

create_app_install_request "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $DSO_PARTY $APP_USER_PARTY $APP_PROVIDER_PARTY $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_ID participant-app-user


