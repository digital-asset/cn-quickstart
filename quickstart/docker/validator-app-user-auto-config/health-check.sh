#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
exec > /proc/1/fd/1 2>&1

if [ ! -f /tmp/onboarding-done ]; then
  /app/onboarding.sh
  touch /tmp/onboarding-done
fi


