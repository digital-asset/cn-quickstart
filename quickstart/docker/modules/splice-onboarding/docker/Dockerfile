# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

FROM sgaunet/jwt-cli:latest AS jwtcli

FROM alpine:3.21
COPY --from=jwtcli /jwt-cli /usr/bin/jwt-cli
RUN apk add --no-cache jq curl bash
ENTRYPOINT ["/bin/bash"]
