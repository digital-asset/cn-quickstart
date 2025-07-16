# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.

# Preview path:
PREVIEW_DIR           ?= .preview

###############################################################################
### Python/Sphinx Environment Setup ###########################################
###############################################################################

.PHONY: poetry-install
poetry-install: .venv
poetry-install: ## use poetry to install python modules

.venv:
	poetry install --no-root
	poetry debug info

.PHONY: clean-venv
clean-venv-dir:
	rm -rfv .venv/

###############################################################################
### Documentation targets #####################################################
###############################################################################

clean-preview-dir: ## remove the .preview/ directory
	rm -rfv ${PREVIEW_DIR}/

render-preview: ## use sphinx to render html version of docs/user/ documentation
render-preview: poetry-install clean-preview-dir
	mkdir -pv ${PREVIEW_DIR}
	poetry run sphinx-build -M html ./docs/ ./${PREVIEW_DIR} --conf-dir .sphinx/ --fresh-env --fail-on-warning

host-preview: ## start http server to enable viewing of render-preview output
host-preview: render-preview
	@echo "Point your browser to http://localhost:8989/html/ to preview user docs... hit ctrl+c to exit"
	python3 -m http.server 8989 -d ${PREVIEW_DIR}


.PHONY: clean-all
clean-all: clean-preview-dir clean-venv-dir
