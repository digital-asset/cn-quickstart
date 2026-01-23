#!/bin/bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

generate_jwt() {
  local sub="$1"
  local aud="$2"
  jwt-cli encode hs256 --s unsafe --p '{"sub": "'"$sub"'", "aud": "'"$aud"'"}'
}

share_file() {
  local relative_path="$1"
  write_to_file "/onboarding/${relative_path}"
}

write_to_file() {
  local absolute_path="$1"
  echo ">>>> writing to ${absolute_path}" >&2
  mkdir -p "$(dirname "${absolute_path}")"
  cat > "${absolute_path}"
}

get_admin_token() {
  local secret=$1
  local clientId=$2
  local tokenUrl=$3

  echo "get_admin_token $clientId $tokenUrl" >&2

  curl -f -s -S "${tokenUrl}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'client_id='${clientId} \
    -d 'client_secret='${secret} \
    -d 'grant_type=client_credentials' \
    -d 'scope=openid' | jq -r .access_token
}

get_user_token() {
  local user=$1
  local password=$2
  local clientId=$3
  local tokenUrl=$4

  echo "get_user_token $user $clientId $tokenUrl" >&2

  curl -f -s -S "${tokenUrl}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'client_id='${clientId} \
    -d 'username='${user} \
    -d 'password='${password} \
    -d 'grant_type=password' \
    -d 'scope=openid' | jq -r .access_token
}

create_user() {
  local token=$1
  local userId=$2
  local userName=$3
  local party=$4
  local participant=$5
  echo "create_user $userId $userName $party $participant" >&2

  code=$(curl_status_code "http://$participant/v2/users/$userId" "$token" "application/json")
  if  [ "$code" == "404" ]; then
    curl_check "http://$participant/v2/users" "$token" "application/json" \
      --data-raw '{
        "user" : {
            "id" : "'$userId'",
            "isDeactivated": false,
            "primaryParty" : "'$party'",
            "identityProviderId": "",
            "metadata": {
               "resourceVersion": "",
                "annotations": {
                    "username" : "'$userName'"
                }
            }
        },
          "rights": [
          ]
      }' | jq -r .user.id
  fi

}

delete_user() {
  local token=$1
  local userId=$2
  local participant=$3
  echo "delete_user $userId $participant" >&2

  code=$(curl_status_code "http://$participant/v2/users/$userId" "$token" "application/json")
  if  [ "$code" == "200" ]; then
    curl_check "http://$participant/v2/users/$userId" "$token" "application/json" -X DELETE
  fi
}

joinByChar() {
  local IFS="$1"
  shift
  echo "$*"
}

function grant_rights() {
  local token=$1
  local userId=$2
  local partyId=$3
  local rights=$4
  local participant=$5
  echo "grant_rights user:$userId party:$partyId $rights $participant" >&2

  read -ra rightsAsArr <<< "$rights"
  local rightsArr=()
  for right in "${rightsAsArr[@]}"; do
    case "$right" in
      "ParticipantAdmin")
        rightsArr+=('{"kind":{"ParticipantAdmin":{"value":{}}}}')
        ;;
      "ActAs")
        rightsArr+=('{"kind":{"CanActAs":{"value":{"party":"'$partyId'"}}}}')
        ;;
      "ReadAs")
        rightsArr+=('{"kind":{"CanReadAs":{"value":{"party":"'$partyId'"}}}}')
        ;;
    esac
  done

  local rightsJson=$(joinByChar "," "${rightsArr[@]}")
  curl_check "http://$participant/v2/users/$userId/rights" "$token" "application/json" \
    --data-raw '{
        "userId": "'$userId'",
        "identityProviderId": "",
        "rights": ['$rightsJson']
    }'
}

update_user() {

  local token=$1
  local userId=$2
  local userName=$3
  local party=$4
  local participant=$5
  echo "update_user $userId $userName $party $participant" >&2
  curl_check "http://$participant/v2/users/$userId" "$token" "application/json" \
    -X PATCH \
    --data-raw '{
      "user" : {
          "id" : "'$userId'",
          "isDeactivated": false,
          "primaryParty" : "'$party'",
          "identityProviderId": "",
          "metadata": {
             "resourceVersion": "",
              "annotations": {
                  "username" : "'$userName'"
              }
          }
      },
      "updateMask": {
          "paths": ["primary_party", "metadata"],
          "unknownFields": {
             "fields": {}
          }
      }
    }' | jq -r .user.id
}

upload_dars() {
  local token=$1
  local participant=$2
  find /canton/dars -type f -name "*.dar" | while read -r file; do
    echo "uploadDar $file $participant" >&2
    curl_check "http://$participant/v2/packages" "$token" "application/octet-stream" \
      --data-binary @"$file"
    echo "Uploaded $file"
  done
}

get_user_party() {
  local token=$1
  local user=$2
  local participant=$3
  echo "get_user_party $user $participant" >&2
  curl_check "http://$participant/v2/users/$user" "$token" "application/json" | jq -r .user.primaryParty
}

get_dso_party_id() {
  local token=$1
  local validator=$2
  echo "get_dso_party_id $validator" >&2
  curl_check "http://$validator/api/validator/v0/scan-proxy/dso-party-id" "$token" "application/json" | jq -r .dso_party_id
}

curl_check() {
  local url=$1
  local token=$2
  local contentType=${3:-application/json}
  shift 3
  local args=("$@")
  echo "$url" >&2
  if [ ${#args[@]} -ne 0 ]; then
    echo "${args[@]}" >&2
  fi

  curlArgs=(-s -S -w "\n%{http_code}" "$url")
  if [ -n "$token" ]; then
    curlArgs+=(-H "Authorization: Bearer $token")
  fi
  curlArgs+=(-H "Content-Type: $contentType")
  curlArgs+=("${args[@]}")
  response=$(curl "${curlArgs[@]}")

  local httpCode=$(echo "$response" | tail -n1 | tr -d '\r')
  local responseBody=$(echo "$response" | sed '$d')

  if [ "$httpCode" -ne "200" ] && [ "$httpCode" -ne "201" ] && [ "$httpCode" -ne "204" ]; then
    echo "Request failed with HTTP status code $httpCode" >&2
    echo "Response body: $responseBody" >&2
    exit 1
  fi

  echo "$responseBody"
}

curl_status_code() {
  local url=$1
  local token=$2
  local contentType=${3:-application/json}
  shift 3
  local args=("$@")
  echo "$url" >&2
  if [ ${#args[@]} -ne 0 ]; then
    echo "${args[@]}" >&2
  fi

  response=$(curl -s -S -w "\n%{http_code}" "$url" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: $contentType" \
      "${args[@]}"
      )

  echo "$response" | tail -n1 | tr -d '\r'
}

# Following functions are not used atm in QS but customer may need them when start building on top of QS
# to support their use-cases. E.g. need to create additional (wallet) users and allocate additional parties.
#
allocate_party() {
  local token=$1
  local partyIdHint=$2
  local participant=$3

  echo "allocate_party $partyIdHint $participant" >&2

  namespace=$(get_participant_namespace "$token" "$participant")

  party=$(curl_check "http://$participant/v2/parties/party?parties=$partyIdHint::$namespace" "$token" "application/json" |
    jq -r '.partyDetails[0].party')

  if [ -n "$party" ] && [ "$party" != "null" ]; then
    echo "party exists $party" >&2
    echo $party
    return
  fi

  curl_check "http://$participant/v2/parties" "$token" "application/json" \
    --data-raw '{
      "partyIdHint": "'$partyIdHint'",
      "displayName" : "'$partyIdHint'",
      "identityProviderId": ""
    }' | jq -r .partyDetails.party
}

get_participant_namespace() {
  local token=$1
  local participant=$2
  echo "get_participant_namespace $participant" >&2
  curl_check "http://$participant/v2/parties/participant-id" "$token" "application/json" |
    jq -r .participantId | sed 's/^participant:://'
}

onboard_wallet_user() {
  local token=$1
  local user=$2
  local party=$3
  local validator=$4
  echo "onboard_wallet_user $user $party $validator $token" >&2
  curl_check "http://$validator/api/validator/v0/admin/users" "$token" "application/json" \
    --data-raw '{
      "party_id": "'$party'",
      "name":"'$user'"
    }'
}
