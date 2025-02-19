#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

export ACCESS_TOKEN=$(curl -fsS "${TOKEN_URL}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${CLIENT_ID}" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}" \
  -d "grant_type=password" \
  -d "scope=openid" | tr -d '\n' | grep -o -E '"access_token"[[:space:]]*:[[:space:]]*"[^"]+' | grep -o -E '[^"]+$')
/app/bin/canton --no-tty -c /app/app.conf
