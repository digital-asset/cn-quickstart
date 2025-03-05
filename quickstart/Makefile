# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

ifneq (,$(wildcard .env))
    include .env
endif

ifneq (,$(wildcard .env.local))
    include .env.local
endif

# Set default goal to 'help' if no target is specified
.DEFAULT_GOAL := help

# Disable gradle daemon:
export GRADLE_OPTS = -Dorg.gradle.daemon=false

# Determine the operating system
KERNEL_NAME := $(shell uname -s)
DOCKER_COMPOSE_ENVFILE := --env-file .env --env-file .env.local
DOCKER_COMPOSE_FILES := -f compose.yaml

ifeq ($(KERNEL_NAME), Darwin)
DOCKER_COMPOSE_OBSERVABILITY_FILES := -f docker/o11y/cadvisor-darwin.yaml -f docker/o11y/compose.yaml
else ifeq ($(KERNEL_NAME), Linux)
DOCKER_COMPOSE_OBSERVABILITY_FILES := -f docker/o11y/cadvisor-linux.yaml -f docker/o11y/compose.yaml
else
DOCKER_COMPOSE_OBSERVABILITY_FILES := -f docker/o11y/compose.yaml
endif

# Define DOCKER_COMPOSE_PROFILES based on environment variables
DOCKER_COMPOSE_PROFILES :=

ifeq ($(LOCALNET_ENABLED),true)
    DOCKER_COMPOSE_PROFILES += --profile localnet --env-file docker/localnet.env
endif

ifeq ($(OBSERVABILITY_ENABLED),true)
    DOCKER_COMPOSE_PROFILES += --profile observability
endif

# Function to run docker-compose with default files and environment
define docker-compose
	docker compose $(DOCKER_COMPOSE_FILES) $(DOCKER_COMPOSE_ENVFILE) $(DOCKER_COMPOSE_PROFILES) $(1)
endef

# Helper to generate an "open URL" target
define open-url-target # $(1) = target name, $(2) = URL to open
.PHONY: $(1)
$(1):
ifeq ($(KERNEL_NAME),Darwin)
	open $(2) &
else ifeq ($(KERNEL_NAME),Linux)
	xdg-open $(2) &
else
	@echo [WARN] $(KERNEL_NAME) is unsupported, please open the following URL in your preferred browser:
	@echo $(2)
endif
endef

SETUP_COMMAND := ./gradlew configureProfiles --no-daemon --console=plain --quiet

# Build targets
.PHONY: build
build: build-frontend build-daml build-docker-images ## Build frontend, backend, Daml model and docker images

.PHONY: build-frontend
build-frontend: ## Build the frontend application
	cd frontend && npm install && npm run build

.PHONY: build-daml
build-daml: ## Build the Daml model
	./gradlew :daml:build

.PHONY: build-docker-images
build-docker-images:
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} build)

.PHONY: create-app-install-request
create-app-install-request: ## Submit an App Install Request from the App User participant node
	docker compose -f docker/app-user-shell/compose.yaml $(DOCKER_COMPOSE_ENVFILE) run --rm create-app-install-request || true

.PHONY: restart-frontend
restart-frontend: build-frontend ## Build and start the application
	$(call docker-compose, restart nginx-app-provider)

# Run targets
.PHONY: start
ifneq ($(strip $(PARTY_HINT)),)
start: build ## Start the application, and observability services if enabled
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} up -d)
else
start:
	@echo "#########################################################################"
	@echo "Looks like this is the first time you are starting the application."
	@echo "Let's configure the local development environment that should be started."
	@echo "You can always change this later by running 'make setup'."
	@echo "#########################################################################"
	@echo ""
	$(SETUP_COMMAND)
	@echo "Environment updated. Please re-run make start."
	@exit 0
endif

.PHONY: stop
stop: ## Stop the application and observability services
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} down)

.PHONY: stop-application
stop-application: ## Stop the application, leaving observability services running
	$(call docker-compose, down)

.PHONY: restart
restart: stop start ## Restart the application

# Utility targets
.PHONY: status
status: ## Show status of Docker containers
	$(call docker-compose, ps)

.PHONY: logs
logs: ## Show logs of Docker containers
	$(call docker-compose, logs)

.PHONY: tail
tail: ## Tail logs of Docker containers
	$(call docker-compose, logs -f)

# Development environment
.PHONY: setup
setup: ## Configure the local development environment (enable DevNet/LocalNet, Observability)
	@echo "Starting setup..."
	$(SETUP_COMMAND)

# Console and shell
.PHONY: console
console: ## Start the Canton console
	docker compose -f docker/canton-console/compose.yaml $(DOCKER_COMPOSE_ENVFILE) run --rm canton-console

.PHONY: clean-console
clean-console: ## Stop and remove the Canton console container
	docker rm -f quickstart-canton-console 2> /dev/null || true

.PHONY: shell
shell: ## Start Daml Shell
	docker compose -f docker/daml-shell/compose.yaml $(DOCKER_COMPOSE_ENVFILE) run --rm daml-shell || true

.PHONY: clean-shell
clean-shell: ## Stop and remove the Daml Shell container
	docker rm -f quickstart-daml-shell 2> /dev/null || true

# Clean targets
.PHONY: clean
clean: ## Clean the build artifacts
	./gradlew clean

.PHONY: clean-docker
clean-docker: clean-shell clean-console ## Stop and remove application Docker containers and volumes
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} down -v)

.PHONY: clean-application
clean-application: ## like clean-docker, but leave observability services running
	$(call docker-compose, down -v)

.PHONY: clean-all
clean-all: clean clean-docker ## Stop and remove all build artifacts, Docker containers and volumes

.PHONY: install-daml-sdk
install-daml-sdk: ## Install the Daml SDK
	./gradlew :daml:installDamlSdk

.PHONY: generate-NOTICES
generate-NOTICES: ## Generate the NOTICES.txt file
	./gradlew generateNotices

# Help target
.PHONY: help
help: ## Show this help message
	@echo "Usage: make [target]"
	@echo
	@echo "Available targets:"
	@grep -E '^(# )?[a-zA-Z_-]+:.*?## .*$$' Makefile | sed -e 's/^# //' | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-20s %s\n", $$1, $$2}'

.PHONY: update-env-sdk-runtime-version
update-env-sdk-runtime-version: ## Helper to update DAML_RUNTIME_VERSION in .env based on daml/daml.yaml sdk-version
	DAML_SDK_VERSION=$$(grep '^sdk-version:' daml/licensing/daml.yaml | awk '{print $$2}'); \
	VERSION=$$(docker run --rm digitalasset/daml-sdk:$$DAML_SDK_VERSION bash -c 'cat /home/daml/.daml/sdk/*/sdk-config.yaml' | grep 'version:' | awk '{print $$2}'); \
	sed -i.bak 's/^DAML_RUNTIME_VERSION=.*/DAML_RUNTIME_VERSION='"$$VERSION/" .env; \
	@echo "Updated DAML_RUNTIME_VERSION to $$VERSION"

# Run arbitrary command with environment variables set
ifneq ($(origin COMMAND), undefined)
.DEFAULT_GOAL := run-command
endif

.PHONY: run-command
run-command:
	$(COMMAND)

# Helpers to open URLs in the browser
# open-app-ui: ## Open the Application UI in the active browser
$(eval $(call open-url-target,open-app-ui,http://localhost:3000))
# open-observe: ## Open the Grafana UI in the active browser
$(eval $(call open-url-target,open-observe,http://localhost:3030))
# open-sv-gateway: ## Open the Super Validator gateway UI in the active browser
$(eval $(call open-url-target,open-sv-gateway,http://localhost:4000))
# open-sv-wallet: ## Open the Super Validator wallet UI in the active browser
$(eval $(call open-url-target,open-sv-wallet,http://wallet.localhost:4000))
# open-sv-interface: ## Open the Super Validator interface UI in the active browser
$(eval $(call open-url-target,open-sv-interface,http://sv.localhost:4000))
# open-sv-scan: ## Open the Super Validator Scan UI in the active browser
$(eval $(call open-url-target,open-sv-scan,http://scan.localhost:4000))
# open-app-user-wallet: ## Open the App User wallet UI in the active browser
$(eval $(call open-url-target,open-app-user-wallet,http://wallet.localhost:2000))