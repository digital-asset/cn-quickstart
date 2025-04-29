#!/bin/bash
set -e

curl -f http://localhost:2${VALIDATOR_ADMIN_API_PORT}/api/validator/readyz
curl -f http://localhost:3${VALIDATOR_ADMIN_API_PORT}/api/validator/readyz
if [ "$ENV" = "local" ]; then
  curl -f http://localhost:4${VALIDATOR_ADMIN_API_PORT}/api/validator/readyz
  curl -f http://localhost:5012/api/scan/readyz
  curl -f http://localhost:5014/api/sv/readyz
fi