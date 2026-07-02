{ pkgs ? import <nixpkgs> {} }:

let
  dpmVersion = "3.5.2";
  system = pkgs.stdenv.hostPlatform.system;
  os = if pkgs.stdenv.isDarwin then "darwin" else "linux";
  arch = if pkgs.stdenv.isAarch64 then "arm64" else "amd64";

  dpmHashes = {
    "x86_64-linux" = "sha256:015npjj4npryg4c7k98g64zlnh4bx4jrwvbkvnhwsj7iniyfjr8c";
    "aarch64-linux" = "sha256:0f9xbpbcl58ml8mfls4wgqc0gcnfkvivjxd20xhfd1lgls8nmbhq";
    "aarch64-darwin" = "sha256:1srqpwglzljk0yiszb90bkp1b368a08dl95j77j72ijwa6hv2igr";
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
