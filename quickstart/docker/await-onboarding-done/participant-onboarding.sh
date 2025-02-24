#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh

# Create admin user for validator oauth2 client as it is required by validator service configuration
# As we are not connected to synchronizer yet (that will happen in validator initialization) we create user without primary party.
# TODO comment just for reviewers remove before merge
# Validator client needs ParticipantAdmin rights otherwise:
#   validator-app-provider.clog   | Caused by: java.lang.RuntimeException: validator_backend app initialization: Ensuring user primary party is allocated failed
#   participant-app-provider.clog | 2025-02-07T21:08:55.617Z [⋮] WARN - c.d.c.a.Authorizer:participant=participant (80eb73955b2066222bfaf9c0edb403e2---) - PERMISSION_DENIED(7,80eb7395): Claims do not authorize the use of administrative services.
APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN=$(get_app_provider_user_token $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_PROVIDER_PARTICIPANT_ADMIN_USER_PASSWORD)
create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME "" participant-app-provider
grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_PROVIDER_VALIDATOR_USER_ID "" ParticipantAdmin participant-app-provider
delete_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" participant_admin participant-app-provider

APP_USER_PARTICIPANT_ADMIN_TOKEN=$(get_app_user_user_token $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_NAME $AUTH_APP_USER_PARTICIPANT_ADMIN_USER_PASSWORD)
create_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID $AUTH_APP_USER_VALIDATOR_USER_NAME "" participant-app-user
grant_rights "$APP_USER_PARTICIPANT_ADMIN_TOKEN" $AUTH_APP_USER_VALIDATOR_USER_ID "" ParticipantAdmin participant-app-user
delete_user "$APP_USER_PARTICIPANT_ADMIN_TOKEN" participant_admin participant-app-user


