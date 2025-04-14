#!/bin/bash
set -e

grpcurl -plaintext localhost:2${CANTON_GRPC_HEALTHCHECK_PORT} grpc.health.v1.Health/Check
grpcurl -plaintext localhost:3${CANTON_GRPC_HEALTHCHECK_PORT} grpc.health.v1.Health/Check
if [ "$LOCALNET_ENABLED" = true ]; then
  grpcurl -plaintext localhost:4${CANTON_GRPC_HEALTHCHECK_PORT} grpc.health.v1.Health/Check
fi