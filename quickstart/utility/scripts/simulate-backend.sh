#!/usr/bin/env bash

set -eou pipefail

cd "$(dirname "$0")"
source '../.env'

# Create the utility parties
UTILITY_ADMIN_USER="utility-admin"
export UTILITY_ADMIN_USER

# get utility-admin tokens
echo "Collecting the tokens for utility parties"
export PROVIDER_UTILITY_ACCESS_TOKEN=$(curl -X POST http://localhost:8081/AppProvider/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${UTILITY_ADMIN_USER}" \
  -d "client_secret=${LEDGER_API_ADMIN_SECRET}" \
  -d "grant_type=client_credentials" \
  -d "scope=daml_ledger_api" | tr -d '\n' | grep -o -E '"access_token"[[:space:]]*:[[:space:]]*"[^"]+' | grep -o -E '[^"]+$')

export USER_UTILITY_ACCESS_TOKEN=$(curl -X POST http://localhost:8081/Org1/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${UTILITY_ADMIN_USER}" \
  -d "client_secret=${LEDGER_API_ADMIN_SECRET}" \
  -d "grant_type=client_credentials" \
  -d "scope=daml_ledger_api" | tr -d '\n' | grep -o -E '"access_token"[[:space:]]*:[[:space:]]*"[^"]+' | grep -o -E '[^"]+$')

# Add the access tokens to the participant-config.json file
tmp=$(mktemp)
jq '.participants.userParticipant += {"access_token":$ENV.USER_UTILITY_ACCESS_TOKEN} | .participants.providerParticipant += {"access_token":$ENV.PROVIDER_UTILITY_ACCESS_TOKEN} | .default_participant += {"access_token":$ENV.PROVIDER_UTILITY_ACCESS_TOKEN}' ../output/participant-config.json > "$tmp" && mv "$tmp" ../output/participant-config.json

echo "Starting the utility UI"
export UTILITY_APP_OPERATOR_PARTY_ID=$(jq -r '.party_participants | with_entries(select(.key | startswith("utility-operator"))) | keys | .[]' ../output/participant-config.json)
touch ../output/operator.json
jq '.party_participants | with_entries(select(.key | startswith("utility-operator"))) | keys | .[]' ../output/participant-config.json > ../output/operator.json

echo "Bootstrapping the utility contracts"
docker compose run --rm execute-accepted-utility-contracts