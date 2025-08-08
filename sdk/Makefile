# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# Set default goal to 'help' if no target is specified:
.DEFAULT_GOAL := help

# Set path to render HTML preview:
PREVIEW_DIR   ?= .preview

# Provide poetry and python with out without making it part of end user nix installation:
POETRY        ?= nix run -I nixpkgs=nixpkgs/25.05 --extra-experimental-features 'nix-command flakes' 'nixpkgs\#poetry' --
PYTHON        ?= nix run -I nixpkgs=nixpkgs/25.05 --extra-experimental-features 'nix-command flakes' 'nixpkgs\#python3' --

###############################################################################
### Python/Sphinx environment targets  ########################################
###############################################################################

.PHONY: poetry-install
poetry-install: .venv
poetry-install: ## use poetry to install python modules

.venv:
	$(POETRY) install --no-root
	$(POETRY) debug info

.PHONY: clean-venv
clean-venv-dir: ## remove python virtual environment directory
	rm -rfv .venv/

###############################################################################
### Documentation targets #####################################################
###############################################################################

.PHONY: clean-preview-dir
clean-preview-dir: ## remove the .preview/ directory
	rm -rfv ${PREVIEW_DIR}/

.PHONY: render-preview
render-preview: ## use sphinx to render html version of docs/user/ documentation
render-preview: poetry-install clean-preview-dir
	mkdir -pv ${PREVIEW_DIR}
	$(POETRY) run sphinx-build -M html ./docs/ ./${PREVIEW_DIR} --conf-dir .sphinx/ --fresh-env --fail-on-warning

.PHONY: host-preview
host-preview: ## start http server to enable viewing of render-preview output
host-preview: render-preview
	@echo "Point your browser to http://localhost:8989/html/ to preview user docs... hit ctrl+c to exit"
	$(PYTHON) -m http.server 8989 -d ${PREVIEW_DIR}


###############################################################################
### Helper targets ############################################################
###############################################################################

.PHONY: clean-all
clean-all: clean-preview-dir clean-venv-dir
clean-all: ## run all clean targets

# Help target
.PHONY: help
help: ## Show this help message
	@echo "Usage: make [target]"
	@echo
	@echo "Available targets:"
	@grep -E '^(# )?[a-zA-Z_-]+:.*?## .*$$' Makefile | sed -e 's/^# //' | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}' | sort
