#!/bin/bash
# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

set -eo pipefail

get_app_provider_admin_token() {
  local secret=$1
  echo "get_app_provider_admin_token" >&2

  curl -f -s -S "${AUTH_APP_PROVIDER_TOKEN_URL}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'client_id=app-provider-validator' \
    -d 'client_secret='${secret} \
    -d 'grant_type=client_credentials' \
    -d 'scope=openid' | jq -r .access_token
}

get_app_user_admin_token() {
  local secret=$1
  echo "get_app_user_admin_token" >&2

  curl -f -s -S "${AUTH_APP_USER_TOKEN_URL}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'client_id=app-user-validator' \
    -d 'client_secret='${secret} \
    -d 'grant_type=client_credentials' \
    -d 'scope=openid' | jq -r .access_token
}

get_app_provider_user_token() {
  local user=$1
  local password=$2
  echo "get_app_provider_user_token $user" >&2

  curl -f -s -S "${AUTH_APP_PROVIDER_TOKEN_URL}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'client_id=app-provider-unsafe' \
    -d 'username='${user} \
    -d 'password='${password} \
    -d 'grant_type=password' \
    -d 'scope=openid' | jq -r .access_token
}

get_app_user_user_token() {
  local user=$1
  local password=$2
  echo "get_app_user_user_token $user" >&2

  curl -f -s -S "${AUTH_APP_USER_TOKEN_URL}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d 'client_id=app-user-unsafe' \
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

  # KV check whether user exist if not then create
  curl_check "http://$participant:7575/v2/users" "$token" "application/json" \
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
}

delete_user() {
  local token=$1
  local userId=$2
  local participant=$3
  echo "delete_user $userId $participant" >&2

  # KV check whether user exist if so then delete
  curl_check "http://$participant:7575/v2/users/$userId" "$token" "application/json" -X DELETE
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
  echo "grant_rights $userId $partyId $rights $participant" >&2
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
  curl_check "http://$participant:7575/v2/users/$userId/rights" "$token" "application/json" \
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
  curl_check "http://$participant:7575/v2/users/$userId" "$token" "application/json" \
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
    curl_check "http://$participant:7575/v2/packages" "$token" "application/octet-stream" \
      --data-binary @"$file"
    echo "Uploaded $file"
  done
}

#allocate_party_and_create_user() {
#  local token=$1
#  local userId=$2
#  local participant=$3
#
#  echo "create_user $userId $participant" >&2
#
#  party=$(get_user_party "$token" "$userId" "$participant")
#  if [ -n "$party" ] && [ "$party" != "null" ]; then
#    echo $party
#    return
#  fi
#
#  party=$(allocate_party "$token" "$userId" "$participant")
#
#  if [ -n "$party" ] && [ "$party" != "null" ]; then
#    curl_check "http://$participant:7575/v2/users" "$token" "application/json" \
#      --data-raw '{
#        "user" : {
#            "id" : "'$userId'",
#            "primaryParty" : "'$party'",
#            "isDeactivated": false,
#            "identityProviderId": ""
#        },
#          "rights": [
#              {
#                  "kind": {
#                      "CanActAs": {
#                          "value": {
#                              "party": "'$party'"
#                          }
#                      },
#                      "CanReadAs": {
#                          "value": {
#                              "party": "'$party'"
#                          }
#                      }
#                  }
#              }
#          ]
#      }' | jq -r .user.primaryParty
#  else
#    echo "Failed to allocate party for user $userId" >&2
#    exit 1
#  fi
#}

get_user_party() {
  local token=$1
  local user=$2
  local participant=$3
  echo "get_user_party $user $participant" >&2
  curl_check "http://$participant:7575/v2/users/$user" "$token" "application/json" | jq -r .user.primaryParty
}

#allocate_party() {
#  local token=$1
#  local partyIdHint=$2
#  local participant=$3
#
#  echo "allocate_party $partyIdHint $participant" >&2
#
#  namespace=$(get_participant_namespace "$token" "$participant")
#
#  party=$(curl_check "http://$participant:7575/v2/parties/party?parties=$partyIdHint::$namespace" "$token" "application/json" |
#    jq -r '.partyDetails[0].party')
#
#  if [ -n "$party" ] && [ "$party" != "null" ]; then
#    echo "party exists $party" >&2
#    echo $party
#    return
#  fi
#
#  curl_check "http://$participant:7575/v2/parties" "$token" "application/json" \
#    --data-raw '{
#      "partyIdHint": "'$partyIdHint'",
#      "displayName" : "'$partyIdHint'",
#      "identityProviderId": ""
#    }' | jq -r .partyDetails.party
#}

#get_participant_namespace() {
#  local token=$1
#  local participant=$2
#  echo "get_participant_namespace $participant" >&2
#  curl_check "http://$participant:7575/v2/parties/participant-id" "$token" "application/json" |
#    jq -r .participantId | sed 's/^participant:://'
#}

#onboard_wallet_user() {
#  local token=$1
#  local user=$2
#  local party=$3
#  local validator=$4
#  echo "onboard_wallet_user $user $party $validator" >&2
#  curl_check "http://$validator:5003/api/validator/v0/admin/users" "$token" "application/json" \
#    --data-raw '{
#      "party_id": "'$party'",
#      "name":"'$user'"
#    }'
#}

get_dso_party_id() {
  local token=$1
  local validator=$2
  curl_check "http://$validator:5003/api/validator/v0/scan-proxy/dso-party-id" "$token" "application/json" | jq -r .dso_party_id
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

  response=$(curl -s -S -w "\n%{http_code}" "$url" \
      -H "Authorization: Bearer $token" \
      -H "Content-Type: $contentType" \
      "${args[@]}"
      )

  local httpCode=$(echo "$response" | tail -n1 | tr -d '\r')
  local responseBody=$(echo "$response" | sed '$d')

  if [ "$httpCode" -ne "200" ]; then
    echo "Request failed with HTTP status code $httpCode" >&2
    echo "Response body: $responseBody" >&2
    exit 1
  fi

  echo "$responseBody"
}
