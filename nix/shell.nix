{ pkgs, ci, nixpkgs }:
let
  inherit (pkgs) stdenv;
  requiredPackages = with pkgs; ([
    # these packages are required both in CI and for local development
    coreutils # provides gdate command needed by Makefile for Docker log timestamp formatting on macOS
    dpm
    jdk21
    nodejs_22
    nodePackages.pnpm
    typescript
  ] ++ (if ci then [ # these packages should only be installed on CI
    circleci-cli
    docutils
    poetry
    python3
    (vale.withStyles (styles: [ styles.google ]))
  ] else [ # these packages are only installed on developer machines locally
    google-cloud-sdk
  ]));
in
pkgs.mkShellNoCC {
  packages = requiredPackages;
  LC_ALL = if stdenv.isDarwin then "" else "C.UTF-8";
  JAVA_HOME="${pkgs.jdk21.home}";
}
