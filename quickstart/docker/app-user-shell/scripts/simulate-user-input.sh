#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# Participant admin will need to provide AppProviderParty to initiate the app install request.
# This script acquires the party automatically base on the assumption that it operates in QS demo topology.
set -eo pipefail

source /app/utils.sh

APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_app_provider_user_token $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_PASSWORD)
APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_user_token $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_PASSWORD)

APP_PROVIDER_PARTY=$(get_user_party "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID participant-app-provider)
APP_USER_PARTY=$(get_user_party "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID participant-app-user)

APP_USER_WALLET_ADMIN_TOKEN=$(get_app_user_user_token $AUTH_APP_USER_WALLET_ADMIN_USER_NAME $AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD)
DSO_PARTY=$(get_dso_party_id "$APP_USER_WALLET_ADMIN_TOKEN" validator-app-user)