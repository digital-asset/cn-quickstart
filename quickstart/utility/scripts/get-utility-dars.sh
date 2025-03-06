#!/usr/bin/env bash

# A script to install the DAR dependencies.

set -eou pipefail
cd "$(dirname "$0")"
source '../.env.utility'
source '../.envrc.private'

version="$UTILITY_VERSION"
url="https://digitalasset.jfrog.io/artifactory/canton-network-utility/dars/canton-network-utility-dars-$version.tar.gz"

if [ ! -f "../dars/canton-network-utility-dars-$version.tar.gz" ]; then
    mkdir -p "../dars"
    echo "== Downloading DAR dependencies for version $version"
    echo "from: $url"
    echo "to: ../dars/canton-network-utility-dars-$version.tar.gz"
    curl --fail -u "$ARTIFACTORY_USER:$ARTIFACTORY_PASSWORD" -L "$url" -o "../dars/canton-network-utility-dars-$version.tar.gz"
    tar -xvf "./dars/canton-network-utility-dars-$version.tar.gz" -C "../dars"
    echo "== Installed DAR dependencies"
else
    echo "== DAR dependencies already installed"
fi

