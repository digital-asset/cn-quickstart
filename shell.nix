{
  system ? builtins.currentSystem,
}:
let
  pins = import ./nix;
  pkgs = import pins.nixpkgs {
    inherit system;
  };
  inherit (pkgs) stdenv lib;

in
pkgs.mkShell {
  nativeBuildInputs =
    with pkgs;
    [
      npins
      jdk21
      nodejs_18
      typescript
    ]
    ++ (lib.optionals stdenv.isDarwin [
      pkgs.libiconv
      pkgs.darwin.apple_sdk.frameworks.Security
    ]);
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
