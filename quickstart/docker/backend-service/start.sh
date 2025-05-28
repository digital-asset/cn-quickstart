#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eou pipefail

# source all scripts from /onboarding/backend-service/on so that env variables exported by them are available in the current shell
for script in /onboarding/backend-service/on/*.sh; do
# shellcheck disable=SC1090
  [ -f "$script" ] && source "$script"
done

tar -xf /backend.tar -C /opt
/opt/backend/bin/backend