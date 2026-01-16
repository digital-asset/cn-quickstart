# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# Set default goal to 'help' if no target is specified:
.DEFAULT_GOAL := help

# Set path to render HTML preview:
PREVIEW_DIR   ?= .preview

# custom CLI args for vale:
VALE_ARGS        ?= --no-wrap
VALE_ALERT_LEVEL ?= error

###############################################################################
### Python/Sphinx environment targets  ########################################
###############################################################################

.PHONY: poetry-install
poetry-install: .venv
poetry-install: ## use poetry to install python modules

.venv:
	poetry install --no-root
	poetry debug info

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
	poetry run sphinx-build -M html ./docs/ ./${PREVIEW_DIR} --conf-dir .sphinx/ --fresh-env --fail-on-warning

.PHONY: host-preview
host-preview: ## start http server to enable viewing of render-preview output
host-preview: render-preview
	@echo "Point your browser to http://localhost:8989/html/ to preview user docs... hit ctrl+c to exit"
	python -m http.server 8989 -d ${PREVIEW_DIR}

###############################################################################
### Linting targets ###########################################################
###############################################################################

.PHONY: run-vale
run-vale:
	vale ./docs/ $(VALE_ARGS) --config ./vale.ini --minAlertLevel=$(VALE_ALERT_LEVEL)

.PHONY: vale-errors
vale-errors: ## check for errors with vale prose linter of user facing docs
vale-errors: run-vale

.PHONY: vale-suggestions
vale-suggestions: ## check for suggestions with vale prose linter of user facing docs
vale-suggestions: VALE_ALERT_LEVEL = suggestions
vale-suggestions: run-vale


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
