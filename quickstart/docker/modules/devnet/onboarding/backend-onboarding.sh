#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# This script is executed by the `splice-onboarding` container. It leverages provided functions from `/app/utils`
# and the resolved environment to onboard a backend service user to a participant (handling user creation and rights assignment),
# and propagating the necessary environment variables to the backend service via the `backend-service.sh` script stored in the shared `onboarding` volume.
# The backend service container sources this shared script during its initialization phase, prior to launching the main process.
#
# DevNet adaptation: Uses external participant endpoint instead of local canton container

set -eo pipefail

source /app/utils.sh

init() {
  local backendUserId=$1
  # DevNet: Connect to external participant instead of local canton
  create_user "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $backendUserId $AUTH_APP_PROVIDER_BACKEND_USER_NAME "" "${PARTICIPANT_HOST}:${PARTICIPANT_PORT}"
  grant_rights "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" $backendUserId $APP_PROVIDER_PARTY "ReadAs ActAs" "${PARTICIPANT_HOST}:${PARTICIPANT_PORT}"
}

# DevNet only supports oauth2 mode
if [ "$AUTH_MODE" == "oauth2" ]; then
  init "$AUTH_APP_PROVIDER_BACKEND_USER_ID"
  share_file "backend-service/on/backend-service.sh" <<EOF
  export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
EOF

else
  echo "ERROR: DevNet only supports oauth2 mode" >&2
  exit 1
fi
