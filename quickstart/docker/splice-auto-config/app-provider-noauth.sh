#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/utils.sh


if [ ! -f /tmp/app-provider-onboarding-dars-uploaded ]; then
  upload_dars "" "canton:3${PARTICIPANT_JSON_API_PORT}"
  touch /tmp/app-provider-onboarding-dars-uploaded
fi

#APP_PROVIDER_PARTY=$(get_user_party "" $AUTH_APP_PROVIDER_VALIDATOR_USER_NAME "canton:3${PARTICIPANT_JSON_API_PORT}")

