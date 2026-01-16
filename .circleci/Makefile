# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# Set default goal to 'help' if no target is specified
.DEFAULT_GOAL := help

# Default to using a nix provided CircleCI cli
# Note: this has been done to reduce noise in ../nix/shell.nix as CircleCI CLI is not needed by consumers of QS
CIRCLECI ?= nix run -I nixpkgs=nixpkgs/25.05 --extra-experimental-features 'nix-command flakes' 'nixpkgs\#circleci-cli' --

validate-configs: ## validate circleci configurations
	cd .. && $(CIRCLECI) config validate
	cd .. && $(CIRCLECI) config validate .circleci/workflow_config.yml

# Help target
.PHONY: help
help: ## Show this help message
	@echo "Usage: make [target]"
	@echo
	@echo "Available targets:"
	@grep -E '^(# )?[a-zA-Z_-]+:.*?## .*$$' Makefile | sed -e 's/^# //' | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'
