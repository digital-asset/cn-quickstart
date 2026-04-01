{ pkgs ? import <nixpkgs> {} }:

let
  dpmVersion = "3.4.11";
  system = pkgs.stdenv.hostPlatform.system;
  os = if pkgs.stdenv.isDarwin then "darwin" else "linux";
  arch = if pkgs.stdenv.isAarch64 then "arm64" else "amd64";

  dpmHashes = {
    "x86_64-linux" = "sha256:1k9csx67s81jzr02qaa6ggh1hps4m1rd3yrpxrp1ly5mv2w2hwqp";
    "aarch64-linux" = "sha256:162nasjhrh8vhn2j3hqswi8w7q1xnn7njcqzdkfk9aq8x1gq9lgw";
    "aarch64-darwin" = "sha256:1i10yqf3a2i5fkhyhkk3dw35jbjdpxp236wdjz31d9z5xz80zg5w";
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
    # but dpm installs it as a separate daml-script component
    mkdir -p $out/cache/components/damlc/${dpmVersion}/damlc-dist-dpm/lib/resources
    ln -s $out/cache/components/daml-script/${dpmVersion}/script-service.jar \
      $out/cache/components/damlc/${dpmVersion}/damlc-dist-dpm/lib/resources/script-service.jar

    rm -rf $out/oci-registry
    rm -rf $out/cache/oci-layout/*

    mkdir -p $out/nix-support
    echo export DPM_HOME=$out > $out/nix-support/setup-hook
    echo export DPM_SDK_VERSION=${dpmVersion} >> $out/nix-support/setup-hook
  '';
}
