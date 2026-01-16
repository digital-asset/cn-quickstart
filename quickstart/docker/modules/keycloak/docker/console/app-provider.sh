#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

export APP_PROVIDER_VALIDATOR_USER_TOKEN=$(curl -fsS "${AUTH_APP_PROVIDER_TOKEN_URL}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${AUTH_APP_PROVIDER_VALIDATOR_CLIENT_ID}" \
  -d 'client_secret='${AUTH_APP_PROVIDER_VALIDATOR_CLIENT_SECRET} \
  -d "grant_type=client_credentials" \
  -d "scope=openid" | tr -d '\n' | grep -o -E '"access_token"[[:space:]]*:[[:space:]]*"[^"]+' | grep -o -E '[^"]+$')
