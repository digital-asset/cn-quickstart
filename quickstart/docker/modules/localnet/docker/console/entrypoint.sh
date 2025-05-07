#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

# source all scripts from /app/pre-startup/on so that env variables exported by them are available in the current shell
for script in /app/pre-startup/on/*.sh; do
  [ -f "$script" ] && source "$script"
done
/app/bin/canton --no-tty -c /app/app.conf