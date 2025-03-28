# Integration Testing

This document describes the **integration test** (or end-to-end test) for `Canton Network Quickstart`. It covers:

- The **overall flow** of the integration test
- **Technologies** used and key tooling decisions
- **Utilities** provided to simplify the integration test cycle
- **How to run** and **extend** the existing integration tests

---

## Overview

The integration tests exercise the entire application in a **fully containerized environment**, verifying that all components (frontend, backend, Daml model, and various supporting services) work together as expected. These tests include UI interactions, API calls, and workflow validation against real services.

Two main approaches are supported, depending on your use case:

1. **`make integration-test`**  
   - Primary target to run the integration tests
   - Spins up a clean, dedicated Docker-in-Docker container for the test.  
   - Builds the project artifacts, creates isolated Docker images, starts the application in that nested container, and then runs end-to-end browser tests.  
   - Cleans up after itself, ensuring a repeatable, isolated test environment.

2. **`make integration-test-ci`**  
   - Utility target, primarily used in CI pipelines.
   - Relies on the application running on **your host machine** rather than in an isolated environment, which means it must be unpolluted from prior interactions.
   - Creates the necessary AppInstallRequests and runs the test container **against your locally running Docker Compose** services.  

---

## Key Technologies and Components

1. **Docker & Docker Compose**  
   - Orchestrate the local environment and all dependent containers (e.g. Canton participant, Keycloak, Observability stack, NGINX, etc.).  
   - The `Makefile` defines the primary commands for building images and controlling container life cycles.

2. **Playwright**  
   - A browser automation framework for end-to-end testing.  
   - Used to interact with the application’s web UIs (e.g., the “AppProvider” and “AppUser” frontends, the wallet UI).

3. **Node.js** integration test harness  
   - The file [`integration-test/setup-and-run-tests.js`](../../quickstart/integration-test/setup-and-run-tests.js) is a custom Node.js script.  
   - It automates building artifacts, preparing a Docker-in-Docker test runner, transferring images into that container, and executing the Playwright tests in a nested environment.

4. **TypeScript Test Suite**  
   - Located in [`integration-test/tests/`](../../quickstart/integration-test/tests/).  
   - [`workflow.spec.ts`](../../quickstart/integration-test/tests/workflow.spec.ts) contains the main scenario, walking through login, wallet top-ups, AppInstallRequests, license creation and payments, etc.

5. **Make Targets**  
   - Several Make targets in the root [`Makefile`](../../quickstart/Makefile) facilitate the workflow:  
     - **`integration-test`**: Runs `setup-and-run-tests.js` for a fully isolated Docker-in-Docker approach.  
     - **`integration-test-ci`**: Assumes the environment is already running locally. Creates the required requests and runs a single ephemeral test container using the host's Docker network.

---

## Provided Utilities

1. **`integration-test/setup-and-run-tests.js`**  
   - A Node.js script that:
     1. Builds the local project (by calling `make build`).
     2. Builds or reuses a Docker-in-Docker image (named `quickstart-end2end-runner` by default).
     3. Spins up that container with a persistent volume for Docker state.
     4. Loads required Docker images inside the nested Docker engine to work around authentication issues for propriatary images, and to avoid repeated pulls.
     5. Launches the application (`make start`) inside the container.
     6. Creates sample requests (e.g., AppInstallRequests).
     7. Runs the Playwright tests in a nested container.
   - Cleans up upon completion to restore the host environment to a clean state.

2. **`integration-test/tests/workflow.spec.ts`**  
   - The main test suite using [Playwright](https://playwright.dev/).  
   - Simulates user actions for both “AppUser” and “AppProvider” roles:
     - Logging into the system
     - Creating and viewing AppInstallRequests
     - Accepting, rejecting, and canceling requests
     - Issuing and renewing licenses
     - Using the wallet UI for payments

3. **`integration-test/package.json`**  
   - Declares dependencies for the integration test environment, primarily `@playwright/test` for browser automation.  
   - Installs TypeScript types (e.g., `@types/node`) if needed for local editing or linting.

---

## How the Testing Setup Works

Below is a high-level summary of the integration test flow:

1. **Build everything**  
   - `make build` compiles the frontend, backend, and Daml model, then builds the Docker images.

2. **Create sample AppInstallRequests**  
   - The test scenarios require multiple `AppInstallRequests` to be created. The script (or `integration-test-ci` target) automatically creates them.

3. **Run Playwright tests**  
   - A container with the official Playwright image (`mcr.microsoft.com/playwright`) is invoked.
   - The tests in `integration-test/tests/` open the application in real browsers, perform logins, navigate flows, and assert expected states.

4. **Tear down**  
   - At the end of `integration-test`, the Docker-in-Docker container is removed, and volumes are retained only if necessary.  

---

## Typical Usage

### Fully Automated, Isolated Testing

```bash
# Runs tests in a single ephemeral Docker environment
make integration-test
```

- This is especially handy if you want a completely **clean environment** each time.  
- Great for verifying local changes without leaving leftover containers or images.

### Testing Against a Locally Running Environment

```bash
# 1. Run the integration tests using the host's environment
make integration-test-ci # depends on, and runs, make start
```

- This workflow is meant to be used in **CI pipelines**.  
- The test container simply attaches to the local Docker network and runs the same suite of Playwright tests.