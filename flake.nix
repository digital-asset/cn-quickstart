{
  description = "CN Quickstart";

  inputs = {
    nixpkgs.url = "nixpkgs/25.11";
    flake-utils.url = "github:numtide/flake-utils/11707dc2f618dd54ca8739b309ec4fc024de578b";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs {
            inherit system;
            overlays = import ./nix/overlays.nix;
            config = { allowUnfree = true; };
          };
        in
        {
          devShells.default = import ./nix/shell.nix { inherit pkgs nixpkgs; ci = false; };
          devShells.ci = import ./nix/shell.nix { inherit pkgs nixpkgs; ci = true; };
        }
      );
}