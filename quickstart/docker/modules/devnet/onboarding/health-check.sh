#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
trap 'touch /tmp/error' ERR
exec > /proc/1/fd/1 2>&1

# Wait for external participant to be reachable
wait_for_participant() {
  local max_retries=60
  local retry=0
  echo "Waiting for external participant at ${PARTICIPANT_HOST}:${PARTICIPANT_PORT}..." >&2
  
  while [ $retry -lt $max_retries ]; do
    if curl -sf "http://${PARTICIPANT_HOST}:${PARTICIPANT_PORT}/v2/version" \
         -H "Host: json-ledger-api.localhost" > /dev/null 2>&1; then
      echo "External participant is reachable" >&2
      return 0
    fi
    retry=$((retry + 1))
    echo "Waiting for participant... attempt $retry/$max_retries" >&2
    sleep 5
  done
  
  echo "ERROR: External participant not reachable after $max_retries attempts" >&2
  return 1
}

if [ ! -f /tmp/all-done ]; then
  ONBOARDING_SCRIPTS_DIR="/app/scripts/on"
  ONBOARDING_TEMP_DIR="/tmp/onboarding-scripts/$(hostname)"

  if [ ! -d "$ONBOARDING_TEMP_DIR" ]; then
    mkdir -p "$ONBOARDING_TEMP_DIR"
  fi

  if [ -f /app/do-init ]; then
    echo "Initializing DevNet onboarding..."
    export DO_INIT=true
  fi
  source /app/utils.sh

  # Wait for external participant before proceeding
  wait_for_participant

  # DevNet: Only app-provider profile is used
  if [ "$APP_PROVIDER_PROFILE" == "on" ]; then
    source /app/app-provider-auth.sh
    if [ "$DO_INIT" == "true" ] && [ ! -f /tmp/app-provider-init-dars-uploaded ]; then
      # Upload DARs to external participant
      upload_dars "$APP_PROVIDER_PARTICIPANT_ADMIN_TOKEN" "${PARTICIPANT_HOST}:${PARTICIPANT_PORT}"
      touch /tmp/app-provider-init-dars-uploaded
    fi
  fi

  echo "Executing onboarding scripts..." >&2

  for script in $(ls "$ONBOARDING_SCRIPTS_DIR"); do
    script_name=$(basename "$script")
    done_file="$ONBOARDING_TEMP_DIR/${script_name}.done"

    if [ ! -f "$done_file" ]; then
      echo "executing $script_name" >&2
      chmod +x "$ONBOARDING_SCRIPTS_DIR/$script"
      "$ONBOARDING_SCRIPTS_DIR/$script"
      echo "$script_name done" >&2
      touch "$done_file"
    fi
  done
  touch /tmp/all-done
fi
