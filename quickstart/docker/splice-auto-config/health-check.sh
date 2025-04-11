#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
exec > /proc/1/fd/1 2>&1

if [ ! -f /tmp/app-provider-onboarding-done ]; then
  /app/app-provider.sh
  touch /tmp/app-provider-onboarding-done
fi

if [ ! -f /tmp/app-user-onboarding-done ]; then
  /app/app-user.sh
  touch /tmp/app-user-onboarding-done
fi

