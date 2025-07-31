#!/usr/bin/env bash
set -euo pipefail

export USER='circleci'
export HOME='/home/circleci'

nix_version='2.24.10'
nix_config="${HOME}/.config/nix"
nix_profile="${HOME}/.nix-profile/etc/profile.d/nix.sh"
nix_env="${HOME}/.nix-bashrc"

actual_cache_keys="${nix_config}/cache-keys"
expected_cache_keys="${HOME}/nix/cache-keys"

# CircleCI automatically runs the bash script at BASH_ENV before any run command;
# copy our nix environment setup file to this path so that we can take advantage
# of this behavior.
#
# BASH_ENV is actually located in a /tmp, so we must always write this file no matter
# the cache state.
echo "Appending ${BASH_ENV} file..."
echo "source ${nix_profile}" >>"${BASH_ENV}"
echo "source ${nix_env}" >>"${BASH_ENV}"

# check cache state to see if nix need to be setup
echo "Checking cache..."
if diff -ur --color=always "${actual_cache_keys}" "${expected_cache_keys}"; then
  echo "Cache is up to date, skipping setup"
  exit
fi
echo "Cache is outdated, setting up nix environment..."

# loads the nix profile if it exists
function load_nix_profile() {
  if [ ! -f "${nix_profile}" ]; then
    echo "No nix profile"
    return 1
  fi

  # shellcheck disable=SC1090
  source "${nix_profile}"
  echo "Nix profile loaded"
}

# config file
echo "Writing nix.conf file..."
mkdir -p "${nix_config}"
tee "${nix_config}/nix.conf" <<EOF
sandbox = false
netrc-file = ${HOME}/.netrc
extra-experimental-features = nix-command flakes
EOF

# install nix
echo "Checking if nix needs to be installed/upgraded..."
if ! load_nix_profile || [ "$(nix --version)" != "nix (Nix) ${nix_version}" ]; then
  echo "Installing nix..."
  sh <(curl -fsSL --retry 8 https://releases.nixos.org/nix/nix-${nix_version}/install) --no-daemon
  load_nix_profile
fi

# prepare nix environment
echo "Preparing nix environment..."
nix --accept-flake-config --no-pure-eval develop "path:.#ci" \
  --profile "${HOME}/.nix-shell" \
  --command echo "Done loading packages"
nix --accept-flake-config --no-pure-eval print-dev-env "path:.#ci" >"${nix_env}"

# clean up
echo "Garbage collecting to reduce cache size..."
nix-store --gc

# CircleCI's will attempt (and fail) to cache this socket if we leave it around
echo "Removing nix socket..."
rm -f /nix/var/nix/gc-socket/socket

# copy the cache keys into the cache
echo "Nix is set up, copying cache keys..."
cp -vr "${expected_cache_keys}" "${actual_cache_keys}"