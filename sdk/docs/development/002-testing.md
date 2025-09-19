# Integration Testing

This document outlines the end-to-end **integration tests** for ``Canton Network Quickstart``, covering:

- Test workflow and architecture
- Core technologies and tooling
- Utilities to streamline testing
- Instructions for running and extending tests

---

## Overview

The integration test suite verifies that all components—frontend, backend, Daml models, and supporting services—operate seamlessly together. These tests encompass browser-driven UI interactions, API request verification, and workflow validation against live service endpoints.

You can execute the integration tests against a locally deployed Canton Network Quickstart instance in test mode using either of the following approaches:
1. Command Line Interface:
   ```shell
   make integration-test
   ```
2. VS Code Integration:
   - Install the Playwright Test extension and run the tests directly from the editor.

Both methods support parallel, repeatable end-to-end test runs without restarting the Quickstart instance.

---

## Key Technologies and Components

1. **Docker Compose**
   - Orchestrate the local environment and all dependent containers (e.g. Canton participant, Keycloak, Observability stack, NGINX, etc.).
   - The ``Makefile`` defines the primary commands for building images and controlling container life cycles.

2. **Playwright**
   - Browser automation framework for end-to-end UI testing
   - Interacts with AppProvider, AppUser frontends, and the wallet UI

3. **TypeScript Test Suite**
   - Located in [``integration-test/tests/``](../../quickstart/integration-test/tests/).
   - [``workflow.spec.ts``](../../quickstart/integration-test/tests/workflow.spec.ts) contains the main Licensing Workflow tests scenario, walking through login, wallet top-ups, AppInstallRequests, license creation and payments, etc.

4. **Make Targets**
   - ``show-integration-test-report``: Serves the Playwright HTML report on port 9323 (``http://0.0.0.0:9323``)

---

## Directory Structure

1. **``tests/workflow.spec.ts``**
   - The main test suite using [Playwright](https://playwright.dev/).
   - Simulates user actions for both “AppUser” and “AppProvider” roles:
     - Logging into the system
     - Creating and viewing AppInstallRequests
     - Accepting, rejecting, and canceling requests
     - Issuing and renewing licenses
     - Using the wallet UI for payments

2. **``tests/login.spec.ts``**
   - Test suite to test:
      -  Logging in to Quickstart as both AppUser and AppProvider (preserving storage state for the latter)
      -  Logging in to Splice Wallet as AppUser

3. **``fixtures``**
   -  Custom fixtures for ``tests/workflow.spec.ts``:
      - **``keycloak``**
         Facade over Keycloak REST admin API
         
      - **``tagProvider``**
         Generates a unique tag for each test run to ensure isolated test data.

      - **``appUserSetup``**
         Automates creation of an AppUser, including Keycloak and Ledger user provisioning, ledger party assignment, and wallet onboarding.

      - **``requestTag``**
         Executes ``make create-app-install-request`` with the generated tag, establishing an AppInstallRequest for workflow tests.

      - **``provider``**
         A session-scoped AppProvider fixture. Authenticates in the Quickstart UI as an app-provider, exposes page object methods for interacting with UI components, and provides state assertions.

      - **``user``**
         A session-scoped AppUser fixture. Authenticates a unique test app-user in the Quickstart UI, provides page object methods for user flows, and supports UI assertions.

4. **``pages``**
   - Contains Page Object Models that represent CN Quickstart ``qs.page.ts`` and Splice Wallet ``wallet.page.ts``.
   - Quickstart Class has multiple sections:
      - ``appInstalls.tab.ts``
      - ``appInstalls.modal.ts``
      - ``licenses.tab.ts``
      - ``login.ts``
   - That promotes code reuse help with test suite maintenance.

4. **``utils``**
   - TypeScript utilities for Keycloak, ledger and wallet REST API interactions.
   - ``AppUserSetup`` class that allows you to 
      - ``create`` - creates unique AppUserSetup for each test invocation
      - ``createAppInstallRequest`` - creates AppInstallRequest with unique test tag for each test invocation. It runs the same shell script as in normal workflow
   - ``RowOps`` helper class for row-level tests data manipulation

---

## How the Testing Setup Works


1. **Build everything**
   ``make build`` 
   Compiles the frontend, backend, and Daml model, then builds the Docker images.

2. **Run Quickstart in the test mode**
   ``make setup`` 
   Enable TEST_MODE and OAUTH2 when prompted, then run ``make start``.

3. **Run tests in VS Code**
   - Open ``quickstart/integration-test`` in VS Code
   - Install the [Playwright Test for VS Code](https://marketplace.visualstudio.com/items?itemName=ms-playwright.playwright) extension.
   - If run for the first time in VS Code invoke action ``Test: Install Playwright`` or ``npm install``
   - Execute tests via the Playwright view

4. **Run tests via CLI**
   ``make integration-test``
