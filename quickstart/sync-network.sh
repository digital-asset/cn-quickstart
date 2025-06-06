#!/bin/bash
#
# A script to update project versions based on a canton network and optionally create a git tag.
#

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Default Configuration ---
NETWORK="dev"
PERFORM_TAG=false
PERFORM_COPY=false

# --- Help Function ---
display_help() {
    echo "Usage: $0 [NETWORK] [--tag] [-h|--help]"
    echo
    echo "Updates local configuration files to match the specified canton network's versions."
    echo
    echo "Arguments:"
    echo "  NETWORK         The network to fetch versions from (e.g., 'dev', 'staging'). Defaults to 'dev'."
    echo "  --tag           If provided, creates a git tag 'splice-VERSION' based on the fetched splice version."
    echo "  --copy          If provided, copies DARs and source-tests from the local sibling repo 'splice'."    
    echo "  -h, --help      Display this help message and exit."
}

# --- Argument Parsing ---
# This loop handles flags and the optional network argument, allowing them in any order.
for arg in "$@"; do
  case $arg in
    -h|--help)
      display_help
      exit 0
      ;;
    --tag)
      PERFORM_TAG=true
      shift # past argument
      ;;
    --copy)
      PERFORM_COPY=true
      shift # past argument
      ;;
    *)
      # Assume any other non-flag argument is the network name.
      if [[ ! "$arg" == -* ]]; then
        NETWORK=$arg
      fi
      shift # past argument
      ;;
  esac
done

# --- Main Execution ---
echo "✅ Starting version synchronization for the '${NETWORK}' network."

# Get versions
echo "Fetching versions..."
INFO_URL="https://docs.${NETWORK}.global.canton.network.sync.global/info"

# Fetch and validate Splice version and Migration ID
SPLICE_VERSION=$(curl -s "$INFO_URL" | jq -r '.synchronizer?.active?.version')
MIGRATION_ID=$(curl -s "$INFO_URL" | jq -r '.synchronizer?.active?.migration_id')

if [ -z "$SPLICE_VERSION" ] || [ "$SPLICE_VERSION" == "null" ]; then
    echo "❌ Error: Could not fetch SPLICE_VERSION for network '$NETWORK'." >&2
    echo "Please check the network name and your internet connection." >&2
    exit 1
fi
echo "  SPLICE_VERSION=$SPLICE_VERSION"
echo "  MIGRATION_ID=$MIGRATION_ID"

# Fetch and validate DAML SDK version
GITHUB_URL="https://raw.githubusercontent.com/hyperledger-labs/splice/refs/heads/release-line-${SPLICE_VERSION}/daml.yaml"
DAML_SDK_VERSION=$(curl -s "$GITHUB_URL" | grep "sdk-version" | awk '{print $2}')

if [ -z "$DAML_SDK_VERSION" ]; then
    echo "❌ Error: Could not fetch DAML_SDK_VERSION for splice version '$SPLICE_VERSION'." >&2
    exit 1
fi
echo "  DAML_SDK_VERSION=$DAML_SDK_VERSION"

# Update daml.yaml files
echo "Updating project files..."
find . -type f -name "daml.yaml" -print0 | while IFS= read -r -d $'\0' file; do
  sed -i -E "s/^(sdk-version: ).*$/\1$DAML_SDK_VERSION/" "$file"
  echo "  $file"
done

# Update .env file
ENV_FILE="./.env"
if [ -f "$ENV_FILE" ]; then
  sed -i -E "s/^(DAML_RUNTIME_VERSION=).*$/\1$DAML_SDK_VERSION/" "$ENV_FILE"
  sed -i -E "s/^(IMAGE_TAG=).*$/\1$SPLICE_VERSION/" "$ENV_FILE"
  echo "  $ENV_FILE"
fi

# Update *net.env file for the specified network
NET_ENV_FILE="./env/${NETWORK}net.env"
if [ -f "$NET_ENV_FILE" ]; then
  # Note: Corrected original script's logic to update MIGRATION_ID with the correct variable
  sed -i -E "s/^(MIGRATION_ID=).*$/\1$MIGRATION_ID/" "$NET_ENV_FILE"
  echo "  $NET_ENV_FILE"
fi

# Sync tag with splice version, if requested
if [ "$PERFORM_TAG" = true ]; then
  TAG_NAME="splice-$SPLICE_VERSION"
  echo "Attempting to create git tag '$TAG_NAME'..."
  git tag -d "$TAG_NAME" 2>/dev/null || true # Remove existing tag if it exists
  if git tag "$TAG_NAME" -m "Works against Canton Network version running $SPLICE_VERSION"; then
    echo "  ⚠️ Created git tag: $TAG_NAME"
  fi
fi

# Update dars and tests
if [ "$PERFORM_TAG" = true ]; then
  rsync -av --existing --exclude daml.yaml ../../splice/daml/dars/ ./daml/dars/
  #rm -rf ./daml/external-test-sources/*
  #cp -r  ../../splice/daml/splice-amulet-test ./daml/external-test-sources/
  #cp -r  ../../splice/daml/splice-wallet-test ./daml/external-test-sources/
  cp -r  ../../splice/token-standard/splice-token-standard-test ./daml/external-test-sources/
fi

echo "✅ Script finished successfully."
