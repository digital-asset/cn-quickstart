#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail
exec > /proc/1/fd/1 2>&1

echo "Start with mode $1"

if [ "$1" == "--init" ] || [ "$1" == "--run-forever" ]; then
  if [ "$1" == "--init" ]; then
    touch /app/do-init
  fi
  tail -f /dev/null
elif [ "$1" == "--exit-on-finish" ]; then
  while [ ! -f /tmp/all-done ]; do sleep 1; done
else
  echo "Invalid argument. Use '--init', '--run-forever', or '--exit-on-finish'."
  exit 1
fi