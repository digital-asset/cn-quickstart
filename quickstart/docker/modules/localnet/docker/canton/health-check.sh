#!/bin/bash
# Copyright (c) 2026 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: Apache-2.0

set -eou pipefail

# The canton image no longer ships grpcurl, so we probe the participants'
# HTTP health server (monitoring.http-health-server) with curl instead.
# curl -f makes a non-2xx response (i.e. an unhealthy node) fail the script.
check() {
  local port="$1"
  echo "Checking ${port}"
  curl -sf "http://localhost:${port}/health" > /dev/null
}

if [ "$APP_USER_PROFILE" = "on" ]; then
  check "2${CANTON_HTTP_HEALTHCHECK_PORT_SUFFIX}"
fi
if [ "$APP_PROVIDER_PROFILE" = "on" ]; then
  check "3${CANTON_HTTP_HEALTHCHECK_PORT_SUFFIX}"
fi
if [ "$SV_PROFILE" = "on" ]; then
  check "4${CANTON_HTTP_HEALTHCHECK_PORT_SUFFIX}"
fi
