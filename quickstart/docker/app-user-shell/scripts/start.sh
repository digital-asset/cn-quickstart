#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

source /app/simulate-user-input.sh
/app/create-app-install-request.sh $APP_PROVIDER_PARTY $APP_USER_PARTY $DSO_PARTY "$APP_USER_WALLET_ADMIN_TOKEN"