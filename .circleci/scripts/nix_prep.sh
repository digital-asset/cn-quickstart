#!/usr/bin/env bash
set -euo pipefail

function main() (
  set -euo pipefail

  local orb_version="$1"
  local artifactory_user="$2"
  local artifactory_password="$3"

  # directories to create
  local dirs=(
    /home/circleci/nix/cache-keys
    /nix
  )

  # directories / files to own
  local own=(
    /home/circleci/nix
    /nix
  )

  # create directories
  echo 'Creating nix directories'
  mkdir -vp "${dirs[@]}"

  # write checksum files
  echo 'Writing checksum files'
  git ls-files --error-unmatch --full-name -s -- *.nix flake.lock ./nix | tee "/home/circleci/nix/cache-keys/nix-checksums"
  tee "/home/circleci/nix/cache-keys/orb-version" <<<"${orb_version}"

  # netrc file
  if [[ -n ${artifactory_user:-} && -n ${artifactory_password:-} ]]; then
    local netrc=/home/circleci/.netrc

    echo "Writing netrc file"
    tee "${netrc}" <<EOF
machine digitalasset.jfrog.io
    login ${artifactory_user}
    password ${artifactory_password}
EOF
    chmod -v 600 "${netrc}"
    own+=("${netrc}")
  fi

  # fix ownerships
  echo 'Fixing ownership'
  chown -vR circleci:circleci "${own[@]}"
)

# # make sure ORB_VERSION is set
# if [ -z "${ORB_VERSION}" ]; then
#   echo "ORB_VERSION var not set"
#   exit 1
# fi

# always run as root
if [ ${EUID} == 0 ]; then
  main "${ORB_VERSION:-}" "${ARTIFACTORY_USER:-}" "${ARTIFACTORY_PASSWORD:-}"
else
  sudo bash -c "$(declare -f main); main \"${ORB_VERSION:-}\" \"${ARTIFACTORY_USER:-}\" \"${ARTIFACTORY_PASSWORD:-}\""
fi