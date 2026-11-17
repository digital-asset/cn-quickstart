{ pkgs ? import <nixpkgs> {} }:

let
  dpmVersion = "3.6.3";
  system = pkgs.stdenv.hostPlatform.system;
  os = if pkgs.stdenv.isDarwin then "darwin" else "linux";
  arch = if pkgs.stdenv.isAarch64 then "arm64" else "amd64";

  dpmHashes = {
    "x86_64-linux" = "sha256:0m1a3y2cc6z35aj1x3i5w0i1vlqcy5iywmk93lf2hkkvrq3g596d";
    "aarch64-linux" = "sha256:0g7k3yljiad38ph1lknyzwr6c2wfcqg4xzp58sclb3h93pfn3w1h";
    "aarch64-darwin" = "sha256:04n4sq9mxwddxzqgmc59j4qc2qwvv03x92sjpnkg68fl8pjxpq6i";
  };
  dpmHash = dpmHashes.${system} or (throw "Unsupported system: ${system}");
in
pkgs.stdenv.mkDerivation {
  pname = "dpm";
  version = dpmVersion;

  src = builtins.fetchurl {
    name = "dpm-sdk-${dpmVersion}.tar.gz";
    url = "https://get.digitalasset.com/install/dpm-sdk/dpm-${dpmVersion}-${os}-${arch}.tar.gz";
    sha256 = dpmHash;
  };
  nativeBuildInputs = [ pkgs.yq-go ];
  dontUnpack = true;
  installPhase = ''
    mkdir -p $out
    tar --strip-components=1 -C $out -xzf $src
    chmod -R u+w $out
    # We need just these components from the SDK, to save space
    yq --inplace \
      '.spec.components |= with_entries(
        select(   .key == "damlc"
               or .key == "daml-script"
               or .key == "codegen-js"
               or .key == "codegen-java"
               or .key == "canton-enterprise"
               or .key == "upgrade-check"
               )
              )' \
      $out/sdk-manifest.yaml
    DPM_HOME=$out $out/bin/dpm bootstrap $out

    # damlc ide expects script-service.jar inside its own resources directory,
    # but dpm installs it as a separate daml-script component. SDK component
    # versions may differ from the SDK release version (e.g. damlc 3.6.2 for SDK 3.6.3).

    script_jar=$(echo $out/cache/components/daml-script/*/script-service.jar | awk '{print $1}')
    if [ -z "$script_jar" ] || [ ! -f "$script_jar" ]; then
      echo "script-service.jar not found under daml-script components" >&2
      exit 1
    fi
    for damlc_dir in $out/cache/components/damlc/*/damlc-dist-dpm; do
      mkdir -p "$damlc_dir/lib/resources"
      ln -sfn "$script_jar" "$damlc_dir/lib/resources/script-service.jar"
    done

    rm -rf $out/oci-registry
    rm -rf $out/cache/oci-layout/*

    mkdir -p $out/nix-support
    echo export DPM_HOME=$out > $out/nix-support/setup-hook
    echo export DPM_SDK_VERSION=${dpmVersion} >> $out/nix-support/setup-hook
  '';
}
