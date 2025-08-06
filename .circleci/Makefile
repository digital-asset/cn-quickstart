# Default to using a nix provided CircleCI cli
# Note: this has been done to reduce noise in ../nix/shell.nix as CircleCI CLI is not needed by consumers of QS
CIRCLECI ?= nix run --extra-experimental-features 'nix-command flakes' 'nixpkgs\#circleci-cli' --

validate-configs: ## validate circleci configuration
	cd .. && $(CIRCLECI) config validate
	cd .. && $(CIRCLECI) config validate .circleci/workflow_config.yml