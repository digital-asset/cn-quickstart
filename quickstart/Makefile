# Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
# SPDX-License-Identifier: 0BSD

# Set default goal to 'help' if no target is specified
.DEFAULT_GOAL := help

# Determine the operating system
KERNEL_NAME := $(shell uname -s)

ifneq (,$(wildcard .env))
    include .env
endif

# export SPLICE_VERSION as IMAGE_TAG mandatory for Splice LocalNet
export IMAGE_TAG=$(SPLICE_VERSION)

ifneq (,$(wildcard .env.local))
    include .env.local
endif

# Determine if the local environment has been configured, if not inject the first-run-setup target
ifneq ($(strip $(PARTY_HINT)),)
FIRST_RUN_DEPENDENCY :=
else
FIRST_RUN_DEPENDENCY := first-run-setup
endif

ifndef MODULES_DIR
  export MODULES_DIR=$(shell pwd)/docker/modules
endif

ifndef LOCALNET_DIR
  export LOCALNET_DIR=$(MODULES_DIR)/localnet
endif

# Print out info about paths:
ifdef PATH_DEBUG_INFO
  $(warning MODULES_DIR=$(MODULES_DIR))
  $(warning LOCALNET_DIR=$(LOCALNET_DIR))
endif

# Default to adding resource constraints for Quickstart compose stack.
RESOURCE_CONSTRAINTS_ENABLED ?= true

# project main compose file
DOCKER_COMPOSE_FILES := -f compose.yaml

############################################################################
####  Setup Splice LocalNet
############################################################################
DOCKER_COMPOSE_FILES += -f ${LOCALNET_DIR}/compose.yaml
DOCKER_COMPOSE_PROFILES := --profile app-provider --profile app-user --profile sv --profile swagger-ui
DOCKER_COMPOSE_ENVFILE := --env-file .env --env-file .env.local --env-file ${LOCALNET_DIR}/compose.env --env-file ${LOCALNET_DIR}/env/common.env
ifeq ($(RESOURCE_CONSTRAINTS_ENABLED),true)
  RESOURCE_CONSTRAINT_CONFIG := -f ${LOCALNET_DIR}/resource-constraints.yaml
endif
ifeq ($(OBSERVABILITY_ENABLED),true)
  DOCKER_COMPOSE_OBSERVABILITY_FILES = -f ${MODULES_DIR}/observability/compose.yaml -f ${MODULES_DIR}/observability/observability.yaml
  DOCKER_COMPOSE_PROFILES += --profile observability
  DOCKER_COMPOSE_ENVFILE += --env-file ${MODULES_DIR}/observability/compose.env
  ifeq ($(KERNEL_NAME), Darwin)
    DOCKER_COMPOSE_OBSERVABILITY_FILES += -f ${MODULES_DIR}/observability/cadvisor-darwin.yaml
  else ifeq ($(KERNEL_NAME), Linux)
    DOCKER_COMPOSE_OBSERVABILITY_FILES += -f ${MODULES_DIR}/observability/cadvisor-linux.yaml
  endif
endif

############################################################################
####  Keycloak (optional)
############################################################################
ifeq ($(AUTH_MODE),oauth2)
    DOCKER_COMPOSE_FILES += -f ${MODULES_DIR}/keycloak/compose.yaml
    DOCKER_COMPOSE_PROFILES += --profile keycloak
    DOCKER_COMPOSE_ENVFILE += --env-file ${MODULES_DIR}/keycloak/compose.env

    ifeq ($(RESOURCE_CONSTRAINTS_ENABLED),true)
      RESOURCE_CONSTRAINT_CONFIG += -f ${MODULES_DIR}/keycloak/resource-constraints.yaml
    endif
endif

############################################################################
####  Splice Onboarding
############################################################################
DOCKER_COMPOSE_FILES += -f ${MODULES_DIR}/splice-onboarding/compose.yaml
ifeq ($(RESOURCE_CONSTRAINTS_ENABLED),true)
  RESOURCE_CONSTRAINT_CONFIG += -f ${MODULES_DIR}/splice-onboarding/resource-constraints.yaml
endif

############################################################################
####  PQS
############################################################################
DOCKER_COMPOSE_FILES += -f ${MODULES_DIR}/pqs/compose.yaml
DOCKER_COMPOSE_PROFILES += --profile pqs-app-provider
DOCKER_COMPOSE_ENVFILE += --env-file ${MODULES_DIR}/pqs/compose.env
ifeq ($(RESOURCE_CONSTRAINTS_ENABLED),true)
  RESOURCE_CONSTRAINT_CONFIG += -f ${MODULES_DIR}/pqs/resource-constraints.yaml
endif
ifeq ($(OBSERVABILITY_ENABLED),true)
  DOCKER_COMPOSE_OBSERVABILITY_FILES += -f ${MODULES_DIR}/pqs/observability.yaml
endif

############################################################################
####  backend-service
############################################################################
ifeq ($(RESOURCE_CONSTRAINTS_ENABLED),true)
  RESOURCE_CONSTRAINT_CONFIG += -f ./docker/backend-service/resource-constraints.yaml
endif
ifeq ($(OBSERVABILITY_ENABLED),true)
  DOCKER_COMPOSE_OBSERVABILITY_FILES += -f ./docker/backend-service/observability.yaml
endif

# Custom overrides if needed
#DOCKER_COMPOSE_FILES += -f <your_custom_compose_overrides.yaml>
#DOCKER_COMPOSE_ENVFILE += --env-file <your_custom_env_overrides_file>

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
build: $(FIRST_RUN_DEPENDENCY) build-frontend build-backend build-daml build-docker-images ## Build frontend, backend, Daml model and docker images

.PHONY: build-frontend
build-frontend: ## Build the frontend application
	cd frontend && npm install && npm run build

.PHONY: build-backend
build-backend: ## Build the backend service
	./gradlew :backend:build

.PHONY: build-daml
build-daml: ## Build the Daml model
	./gradlew :daml:build distTar

.PHONY: check-docker
check-docker: ## Check Docker and Docker Compose versions
	@{ \
    	  min_docker="27.0.0"; \
    	  have_docker=$$(docker version --format '{{.Client.Version}}' | sed 's/-.*//'); \
    	  if ! printf '%s\n%s\n' "$$min_docker" "$$have_docker" | sort -V -C 2>/dev/null; then \
    	    echo "✗ Docker $$have_docker too old – need >= $$min_docker"; exit 1; \
    	  else \
    	    echo "✓ Docker $$have_docker OK"; \
    	  fi; \
    }
	@{ \
    	  min_compose="2.27.0"; \
    	  have_compose=$$(docker compose version --short | sed 's/-.*//'); \
    	  if ! printf '%s\n%s\n' "$$min_compose" "$$have_compose" | sort -V -C 2>/dev/null; then \
    	    echo "✗ Compose $$have_compose too old – need >= $$min_compose"; exit 1; \
    	  else \
    	    echo "✓ Compose $$have_compose OK"; \
    	  fi; \
   	}

.PHONY: test
test: test-daml ## Run unit tests

.PHONY: test-daml
test-daml: ## Run daml tests
	./gradlew :daml:testDaml

.PHONY: build-docker-images
build-docker-images:
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) build)

.PHONY: create-app-install-request
create-app-install-request: DOCKER_COMPOSE_FILES = -f docker/create-app-install-request/compose.yaml
create-app-install-request: DOCKER_COMPOSE_PROFILES =
create-app-install-request: ## Submit an App Install Request from the App User participant node
	$(call docker-compose, run --rm container)

.PHONY: restart-backend
restart-backend: build-backend ## Build and start the application
	$(call docker-compose, rm -s -f backend-service)
	$(call docker-compose, rm -s -f register-app-user-tenant)
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) up -d --no-recreate)

.PHONY: restart-frontend
restart-frontend: build-frontend ## Build and start the application
	$(call docker-compose, restart nginx)

# Run targets
.PHONY: start
ifeq ($(OBSERVABILITY_ENABLED),true)
ifneq ($(SKIP_DOWNLOADS),true) # treat “true” (or any non‑empty value) as “enabled”
start: fetch-grafana-dashboards
endif
endif
start: $(FIRST_RUN_DEPENDENCY) build ## Start the application, and observability services if enabled
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) up -d --no-recreate)

.PHONY: start-vite-dev
start-vite-dev: ## Start the frontend application in development mode
	@echo "Starting application and Vite frontend development server..."
	make start && cd frontend && npm run dev

.PHONY: stop
stop: ## Stop the application and observability services
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) down)

.PHONY: stop-application
stop-application: ## Stop the application, leaving observability services running
	$(call docker-compose, down)

.PHONY: restart
restart: stop start ## Restart the application

# Utility targets
.PHONY: status
status: ## Show status of Docker containers
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) ps)

.PHONY: compose-config
compose-config:
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) config)

.PHONY: logs
logs: ## Show logs of Docker containers
	$(call docker-compose, logs)

.PHONY: tail
tail: ## Tail logs of Docker containers
	$(call docker-compose, logs -f)

.PHONY: capture-logs
capture-logs: ## Monitor docker events and capture logs
	@network_name="quickstart"; \
	canton_services=("canton" "splice" "backend-service"); \
	mkdir -p ./logs; rm -rf ./logs/*; \
	docker events -f type=container \
	    -f event=start \
	    -f event=stop \
	    -f event=restart \
	    -f event=kill \
	    -f event=die \
	    -f event=destroy \
	    -f event=health_status \
	    -f event=oom \
	    --format '{{.ID}} {{.Time}} {{.Actor.Attributes.name}} {{.Status}}' | \
	while read -r cid time service_name status; do \
	    network_attached=$$(docker inspect --format '{{json .NetworkSettings.Networks}}' "$$cid" 2>/dev/null | \
	        jq -r 'keys[] | select(. | contains ("'"$$network_name"'"))'); \
	    if [ -z "$$network_attached" ]; then \
	        continue; \
	    fi; \
	    echo "$$(date -u -d "@$$time" +"%Y-%m-%dT%H:%M:%S") $$service_name $$status"; \
	    if [ "$$status" = "start" ]; then \
	        echo ">> $$service_name"; \
	        ext=".log"; \
	        for svc in "$${canton_services[@]}"; do \
	            if [ "$$service_name" = "$$svc" ]; then \
	                ext=".clog"; \
	                break; \
	            fi; \
	        done; \
	        docker logs -f "$$cid" >> "./logs/$$service_name$$ext" 2>&1 & \
	    fi; \
done | tee -a "logs/compose.log" 2>&1

# Development environment
.PHONY: setup
setup: ## Configure the local development environment (enable DevNet/LocalNet, Observability)
	@echo "Starting local environment setup tool..."
	$(SETUP_COMMAND)

.PHONY: first-run-setup
first-run-setup:
	@echo "#########################################################################"
	@echo "Looks like your local configuration is missing or stale."
	@echo "Let's configure the local development environment before proceeding."
	@echo "You can always change your configuration later by running 'make setup'."
	@echo "#########################################################################"
	@echo ""
	$(SETUP_COMMAND)
	@echo "Environment file generated, Please re-run your previous command to continue."
	@exit 2

.PHONY: integration-test-env-init
integration-test-env-init: DOCKER_COMPOSE_FILES = -f ./integration-test/compose.yaml
integration-test-env-init: DOCKER_COMPOSE_PROFILES =
integration-test-env-init: ## Generate environment variables for integration tests
	rm -f ./integration-test/.generated.env && \
	rm -f ./integration-test/.generated.docker.override.env && \
	$(call docker-compose, run --rm environment-init)

.PHONY: integration-test
integration-test: DOCKER_COMPOSE_FILES = -f ./integration-test/compose.yaml
integration-test: DOCKER_COMPOSE_PROFILES =
integration-test: ## Run integration tests
integration-test: integration-test-env-init
	@if [ "$(AUTH_MODE)" = "shared-secret" ]; then \
		echo "Integration tests supported only for AUTH_MODE=oauth2."; \
		exit 1; \
	fi
	@cd integration-test && \
	npm install
	$(call docker-compose, run --rm integration-tests)

.PHONY: show-integration-test-report
show-integration-test-report: ## Show the integration test report
	@cd integration-test && \
	npx playwright show-report --port=9323 --host=0.0.0.0

# Console
.PHONY: canton-console
canton-console: ## Start the Canton console
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) run --rm --name canton-console console)

.PHONY: clean-canton-console
clean-canton-console: ## Stop and remove the Canton console container
	docker rm -f canton-console 2> /dev/null || true

# Shell
.PHONY: shell
shell: DOCKER_COMPOSE_FILES = -f ${MODULES_DIR}/daml-shell/compose.yaml
shell: DOCKER_COMPOSE_ENVFILE += --env-file ${MODULES_DIR}/daml-shell/compose.env
shell: DOCKER_COMPOSE_PROFILES =
shell: ## Start Daml Shell
	$(call docker-compose, run --rm daml-shell)

.PHONY: clean-shell
clean-shell: ## Stop and remove the Daml Shell container
	docker rm -f quickstart-daml-shell 2> /dev/null || true

# Clean targets
.PHONY: clean
clean: ## Clean the build artifacts
	./gradlew clean

.PHONY: clean-docker
clean-docker: clean-shell clean-canton-console ## Stop and remove application Docker containers and volumes
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) down -v)

.PHONY: clean-application
clean-application: ## like clean-docker, but leave observability services running
	$(call docker-compose, down -v)

.PHONY: clean-all
clean-all: clean clean-docker ## Stop and remove all build artifacts, Docker containers and volumes

.PHONY: install-daml-sdk
install-daml-sdk: ## Install the Daml SDK
	./gradlew :daml:installDamlSdk

.PHONY: check-daml-sdk
check-daml-sdk: ## Install the Daml SDK
	./gradlew :daml:verifyDamlSdkVersion

.PHONY: fetch-grafana-dashboards
fetch-grafana-dashboards: ## Download Grafana dashboards
	./gradlew fetchGrafanaDashboards

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
	echo "Updated DAML_RUNTIME_VERSION to $$VERSION"

# Run arbitrary command with environment variables set
ifneq ($(origin COMMAND), undefined)
.DEFAULT_GOAL := run-command
endif

.PHONY: run-command
run-command:
	$(COMMAND)

# Helpers to open URLs in the browser
# open-app-ui: ## Open the Application UI in the active browser
$(eval $(call open-url-target,open-app-ui,http://app-provider.localhost:3000))
# open-observe: ## Open the Grafana UI in the active browser
$(eval $(call open-url-target,open-observe,http://localhost:3030))
# open-sv-wallet: ## Open the Super Validator wallet UI in the active browser
$(eval $(call open-url-target,open-sv-wallet,http://wallet.localhost:4000))
# open-sv-interface: ## Open the Super Validator interface UI in the active browser
$(eval $(call open-url-target,open-sv-interface,http://sv.localhost:4000))
# open-sv-scan: ## Open the Super Validator Scan UI in the active browser
$(eval $(call open-url-target,open-sv-scan,http://scan.localhost:4000))
# open-app-user-wallet: ## Open the App User wallet UI in the active browser
$(eval $(call open-url-target,open-app-user-wallet,http://wallet.localhost:2000))
# open-swagger-ui: ## Open Swagger UI to view Canton JSON API V2 Open API in the active browser
$(eval $(call open-url-target,open-swagger-ui,http://localhost:9090))