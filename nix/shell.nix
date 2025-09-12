{ pkgs, ci }:
let
  inherit (pkgs) stdenv;
  requiredPackages = with pkgs; ([ # these packages are required both in CI and for local development
    jdk21
    nodejs_20
    typescript
  ] ++ (if ci then [ # these packages should only be installed on CI
    circleci-cli
    docutils
    poetry
    python3
    (vale.withStyles (styles: [ styles.google ]))
  ] else [ # these packages are only installed on developer machines locally
  ]));
in
pkgs.mkShell {
  packages = requiredPackages;
  LC_ALL = if stdenv.isDarwin then "" else "C.UTF-8";
  env = {
  };

  shellHook = ''
    export JAVA_HOME="$(readlink -e $(type -p javac) | sed  -e 's/\/bin\/javac//g')"

    # there is a nix bug that the directory deleted by _nix_shell_clean_tmpdir can be the same as the general $TEMPDIR
    eval "$(declare -f _nix_shell_clean_tmpdir | sed 's/_nix_shell_clean_tmpdir/orig__nix_shell_clean_tmpdir/')"
    _nix_shell_clean_tmpdir() {
        orig__nix_shell_clean_tmpdir "$@"
        mkdir -p "$TEMPDIR" # ensure system TEMPDIR still exists
    }
    '';
}
