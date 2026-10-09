#!/usr/bin/env bash
# Generate TypeScript bindings for the licensing DAR (and its splice
# Token-Standard transitive deps). Output is one workspace package per
# input DAR under daml-bindings/, consumed via pnpm-workspace.yaml.
#
# Prerequisites:
#   - dpm on PATH. Installed by `make build-daml` from quickstart/.
#   - quickstart-licensing-0.0.1.dar built. Same target.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
QUICKSTART_DIR="$(cd "$EXAMPLE_DIR/../.." && pwd)"

OUTPUT_DIR="$EXAMPLE_DIR/daml-bindings"

# Minimum DAR set needed for the AppInstall_CreateLicense path:
#   - licensing: AppInstall + AppInstall_CreateLicense + License.
#   - splice-api-token-metadata-v1: MetadataV1.Metadata, referenced by
#     LicenseParams.meta. Without this, the typed choice argument can't
#     resolve.
DARS=(
  "$QUICKSTART_DIR/daml/licensing/.daml/dist/quickstart-licensing-0.0.1.dar"
  "$QUICKSTART_DIR/daml/dars/splice-api-token-metadata-v1-1.0.0.dar"
)

for dar in "${DARS[@]}"; do
  if [ ! -f "$dar" ]; then
    echo "ERROR: missing DAR: $dar" >&2
    echo "Hint: run 'make build-daml' from quickstart/ first." >&2
    exit 1
  fi
done

if ! command -v dpm >/dev/null 2>&1; then
  echo "ERROR: dpm not on PATH. Install via 'make build-daml' from quickstart/." >&2
  exit 1
fi

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"
dpm codegen-js "${DARS[@]}" -o .

echo "==> Codegen output:"
ls -1 "$OUTPUT_DIR"
echo
echo "==> Done. Run 'pnpm install' to pick up the workspace packages."
