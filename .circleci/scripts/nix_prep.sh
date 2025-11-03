#!/usr/bin/env bash
set -euo pipefail

function main() (
  set -euo pipefail

  local orb_version="$1"

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

  # fix ownerships
  echo 'Fixing ownership'
  chown -vR circleci:circleci "${own[@]}"
)

# always run as root
if [ ${EUID} == 0 ]; then
  main "${ORB_VERSION:-}"
else
  sudo bash -c "$(declare -f main); main \"${ORB_VERSION:-}\""
fi