#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eou pipefail

while [ ! -f /work/.generated.env ]; do
  echo "Waiting for .generated.env to be available..."
  sleep 1
done

npm install
npx playwright test --output /work/test-results --trace=retain-on-first-failure