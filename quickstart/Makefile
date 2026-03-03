# Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
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
ifeq ($(PQS_APP_USER_PROFILE),on)
  DOCKER_COMPOSE_PROFILES += --profile pqs-app-user
endif
ifeq ($(PQS_SV_PROFILE),on)
  DOCKER_COMPOSE_PROFILES += --profile pqs-sv
endif
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
ifeq ($(DEBUG_ENABLED),true)
  DOCKER_COMPOSE_FILES += -f ./docker/backend-service/debug.yaml
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

.PHONY: docker-available
docker-available: ## Check if Docker CLI exists and is running
	@{ \
    	  if ! command -v docker >/dev/null 2>&1; then \
    	    echo "✗ Docker CLI not found – please install Docker."; \
    	    echo "  See: https://docs.docker.com/engine/install/"; \
    	    exit 1; \
    	  fi; \
    	  if ! docker info >/dev/null 2>&1; then \
    	    echo "✗ Docker does not appear to be running – please start it."; \
    	    echo "  See: https://docs.docker.com/engine/daemon/start/"; \
    	    exit 1; \
    	  fi; \
    	  echo "✓ Docker is installed and running"; \
    }

.PHONY: check-docker
check-docker: docker-available ## Check Docker and Docker Compose versions
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
test-daml: build-daml
	./gradlew :daml:testDaml

.PHONY: build-docker-images
build-docker-images: docker-available
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) build)

.PHONY: create-app-install-request
create-app-install-request: DOCKER_COMPOSE_FILES = -f docker/create-app-install-request/compose.yaml
create-app-install-request: DOCKER_COMPOSE_PROFILES =
create-app-install-request: docker-available ## Submit an App Install Request from the App User participant node
	$(call docker-compose, run --rm container)

.PHONY: restart-backend
restart-backend: build-backend docker-available ## Build and start the application
	$(call docker-compose, rm -s -f backend-service)
	$(call docker-compose, rm -s -f register-app-user-tenant)
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) up -d --no-recreate register-app-user-tenant)

.PHONY: restart-frontend
restart-frontend: build-frontend docker-available ## Build and start the application
	$(call docker-compose, restart nginx)

.PHONY: restart-service
restart-service:
	@if [ -z "${SERVICE}" ]; then \
	  echo "Usage: make restart-service SERVICE=<service>"; exit 1; \
	fi
	$(call docker-compose, rm -s -f ${SERVICE})
	$(call docker-compose, up -d --no-recreate ${SERVICE})

# Run targets
.PHONY: start
ifeq ($(OBSERVABILITY_ENABLED),true)
ifneq ($(SKIP_DOWNLOADS),true) # treat “true” (or any non‑empty value) as “enabled”
start:
endif
endif
start: $(FIRST_RUN_DEPENDENCY) build ## Start the application, and observability services if enabled
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) up -d --no-recreate)

.PHONY: start-vite-dev
start-vite-dev: ## Start the frontend application in development mode
	make start vite-dev

.PHONY: vite-dev
vite-dev:
	@echo "Starting Vite frontend development server..."
	cd frontend && npm run dev

.PHONY: stop
stop: docker-available ## Stop the application and observability services
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) down)

.PHONY: stop-application
stop-application: docker-available ## Stop the application, leaving observability services running
	$(call docker-compose, down)

.PHONY: restart
restart: stop start ## Restart the application

# Utility targets
.PHONY: status
status: docker-available ## Show status of Docker containers
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) ps)

.PHONY: compose-config
compose-config: docker-available
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) config)

.PHONY: logs
logs: docker-available ## Show logs of Docker containers
	$(call docker-compose, logs)

.PHONY: tail
tail: docker-available ## Tail logs of Docker containers
	$(call docker-compose, logs -f)

.PHONY: capture-logs
capture-logs: docker-available ## Monitor docker events and capture logs
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
	    --format '{{.Actor.ID}} {{.Time}} {{.Actor.Attributes.name}} {{.Action}}' | \
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

.PHONY: integration-test
integration-test: docker-available ## Run integration tests
	@docker rm -f environment-init 2> /dev/null || true
	@if [ "$(TEST_MODE)" = "off" ]; then \
		echo "To run the integration tests Quickstart must be started in test mode. Please run \"make setup\" and enable TEST_MODE when prompted"; \
		exit 1; \
	fi
	@if [ "$(AUTH_MODE)" = "shared-secret" ]; then \
		echo "Integration tests supported only for AUTH_MODE=oauth2."; \
		exit 1; \
	fi
	@cd integration-test && \
	npm install && \
	npx playwright test

.PHONY: show-integration-test-report
show-integration-test-report: ## Show the integration test report
	@cd integration-test && \
	npx playwright show-report --port=9323 --host=0.0.0.0

# Console
.PHONY: canton-console
canton-console: docker-available ## Start the Canton console
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) run --rm --name canton-console console)

.PHONY: clean-canton-console
clean-canton-console: docker-available ## Stop and remove the Canton console container
	docker rm -f canton-console 2> /dev/null || true

# Shell
.PHONY: shell
shell: DOCKER_COMPOSE_FILES = -f ${MODULES_DIR}/daml-shell/compose.yaml
shell: DOCKER_COMPOSE_ENVFILE += --env-file ${MODULES_DIR}/daml-shell/compose.env
shell: DOCKER_COMPOSE_PROFILES =
shell: docker-available ## Start Daml Shell
	$(call docker-compose, run --rm daml-shell)

.PHONY: clean-shell
clean-shell: docker-available ## Stop and remove the Daml Shell container
	docker rm -f quickstart-daml-shell 2> /dev/null || true

# Clean targets
.PHONY: clean
clean: ## Clean the build artifacts
	./gradlew clean

.PHONY: clean-docker
clean-docker: clean-shell clean-canton-console ## Stop and remove application Docker containers and volumes
	$(call docker-compose, ${DOCKER_COMPOSE_OBSERVABILITY_FILES} $(RESOURCE_CONSTRAINT_CONFIG) down -v)

.PHONY: clean-application
clean-application: docker-available ## like clean-docker, but leave observability services running
	$(call docker-compose, down -v)

.PHONY: clean-all
clean-all: clean clean-docker clean-all-docker ## Stop and remove all build artifacts, Docker containers and volumes

.PHONY: clean-all-docker
clean-all-docker: docker-available ## Stop and remove all Docker containers network and volumes created by the project
	@echo "⛔ Removing QS containers (network=$(DOCKER_NETWORK))..."
	@cids="$$(docker ps -aq --filter "network=$(DOCKER_NETWORK)")"; \
    if [ -n "$$cids" ]; then docker rm -f $$cids; else echo "  (none)";fi

	@echo "🧹 Removing $(DOCKER_NETWORK) network..."
	@nids="$$(docker network ls -q --filter "name=$(DOCKER_NETWORK)")"; \
	if [ -n "$$nids" ]; then docker network rm $$nids; else echo "  (none)";fi

	@echo "🗑️  Removing QS volumes (label=$(DOCKER_NETWORK))..."
	@vids="$$(docker volume ls -q --filter "name=^$(DOCKER_NETWORK)")"; \
	if [ -n "$$vids" ]; then docker volume rm $$vids; else echo "  (none)";fi

.PHONY: install-daml-sdk
install-daml-sdk: ## Install the Daml SDK
	./gradlew :daml:installDamlSdk

.PHONY: check-daml-sdk
check-daml-sdk: ## Install the Daml SDK
	./gradlew :daml:verifyDamlSdkVersion

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

## Conditional targets for CI/CD, only available if CI == true
ifeq ($(CI),true)

# Populate .env.local for CI/CD purposes
.PHONY: ci-create-env-local
ci-create-env-local:
	echo "OBSERVABILITY_ENABLED=true" > .env.local
	echo "AUTH_MODE=oauth2" >> .env.local
	echo "PARTY_HINT=quickstart-circleci-1" >> .env.local
	echo "TEST_MODE=on" >> .env.local

# Install playwright dependencies in CI/CD
.PHONY: ci-install-playwright
ci-install-playwright:
	cd integration-test \
		&& npm ci \
		&& npx playwright install chromium --with-deps

endif

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
