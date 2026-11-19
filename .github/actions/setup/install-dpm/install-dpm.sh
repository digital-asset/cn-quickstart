#!/usr/bin/env bash
# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# Installs dpm 3.6.3 under RUNNER_TEMP and exports DPM_HOME, DPM_SDK_VERSION
# and PATH for the rest of the job.
#
# The tarball is the one nix/dpm.nix fetches. expected_dpm_sha256 is the
# x86_64-linux hash in that file (sha256:0m1a3y2cc6z35aj1x3i5w0i1vlqcy5iywmk93lf2hkkvrq3g596d)
# converted to hex. Keep the two in lockstep when bumping the version.
#
# 3.6.3's sdk-manifest.yaml names components codegen and canton-open-source.
# nix/dpm.nix still selects codegen-js, codegen-java and canton-enterprise,
# which this manifest does not contain. The trim below keeps the components
# the quickstart build actually runs: damlc, daml-script, codegen and
# upgrade-check. Canton comes from the published Docker images, so the
# open-source Canton component is left out.
#
# After bootstrap, script-service.jar is linked into damlc's resources
# directory. Component directories use the component version (damlc and
# daml-script are 3.6.2 inside the 3.6.3 SDK). damlc-dist-dpm/lib/resources
# is a symlink to damlc-dist-dpm/resources, which is where damlc looks.

set -euo pipefail

version='3.6.3'
yq_version='v4.47.1'
dest="${RUNNER_TEMP}/dpm"
yq="${RUNNER_TEMP}/yq"

expected_dpm_sha256='cda4f206ce7b4e281c1d6956ee63f10cd31d22e0258e1ea42ae31bc6841f2a54'
# Published checksum for mikefarah/yq v4.47.1, linux amd64.
expected_yq_sha256='0fb28c6680193c41b364193d0c0fc4a03177aecde51cfc04d506b1517158c2fb'

mkdir -p "${dest}"
curl -fsSL -o "${dest}/dpm.tar.gz" \
  "https://get.digitalasset.com/install/dpm-sdk/dpm-${version}-linux-amd64.tar.gz"
echo "${expected_dpm_sha256}  ${dest}/dpm.tar.gz" | sha256sum -c -
tar --strip-components=1 -C "${dest}" -xzf "${dest}/dpm.tar.gz"
rm "${dest}/dpm.tar.gz"

curl -fsSL -o "${yq}" \
  "https://github.com/mikefarah/yq/releases/download/${yq_version}/yq_linux_amd64"
echo "${expected_yq_sha256}  ${yq}" | sha256sum -c -
chmod +x "${yq}"

required_components=(damlc daml-script codegen upgrade-check)
for key in "${required_components[@]}"; do
  if ! "${yq}" -e ".spec.components | has(\"${key}\")" "${dest}/sdk-manifest.yaml" >/dev/null; then
    echo "sdk-manifest.yaml for dpm ${version} is missing component ${key}." >&2
    exit 1
  fi
done

"${yq}" --inplace \
  '.spec.components |= with_entries(
    select(   .key == "damlc"
           or .key == "daml-script"
           or .key == "codegen"
           or .key == "upgrade-check"
           )
          )' \
  "${dest}/sdk-manifest.yaml"

DPM_HOME="${dest}" "${dest}/bin/dpm" bootstrap "${dest}"

# The bundled registry is only needed for bootstrap.
rm -rf "${dest}/oci-registry" "${dest}/cache/oci-layout"

shopt -s nullglob
jars=("${dest}/cache/components/daml-script/"*/script-service.jar)
if [ "${#jars[@]}" -eq 0 ] || [ ! -f "${jars[0]}" ]; then
  echo "script-service.jar not found under daml-script components" >&2
  exit 1
fi
script_jar="${jars[0]}"

linked=0
for damlc_dir in "${dest}/cache/components/damlc/"*/damlc-dist-dpm; do
  if [ ! -d "${damlc_dir}" ]; then
    continue
  fi
  # lib/resources symlinks to ../resources on the 3.6.3 SDK.
  mkdir -p "${damlc_dir}/lib/resources"
  ln -sfn "${script_jar}" "${damlc_dir}/lib/resources/script-service.jar"
  if [ ! -f "${damlc_dir}/lib/resources/script-service.jar" ]; then
    echo "failed to link script-service.jar into ${damlc_dir}/lib/resources" >&2
    exit 1
  fi
  linked=1
done
if [ "${linked}" -ne 1 ]; then
  echo "damlc resources directory not found; cannot link script-service.jar" >&2
  exit 1
fi

echo "${dest}/bin" >> "${GITHUB_PATH}"
{
  echo "DPM_HOME=${dest}"
  echo "DPM_SDK_VERSION=${version}"
} >> "${GITHUB_ENV}"
