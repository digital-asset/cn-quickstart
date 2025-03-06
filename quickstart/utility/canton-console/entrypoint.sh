#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

/app/bin/canton run /app/env/bootstrap-utility.sc --no-tty -c /app/env/console.conf --log-level-stdout=DEBUG --log-file-appender=off --log-encoder=json
