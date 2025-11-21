.. _quickstart-project-structure-guide:

===========================================
Canton Network quickstart project structure
===========================================

Overview
========

The CN Quickstart provides a complete development environment for building Canton Network applications.
It combines build tools (Gradle, Make), deployment infrastructure (Docker Compose), and a reference application to accelerate your development.

The project demonstrates Canton development patterns through a licensing application while providing the scaffolding you need for your own applications.

Reference application
=====================

The Quickstart includes a licensing application with four parties:

* **Application Provider** - Sells licenses
* **Application User** - Buys licenses
* **DSO Party** - Operates the payment system (Super Validators in CN)
* **Amulet** - Token system for payments (Canton Coin by default)

The Provider and User are independent parties, each requiring their own validator node to maintain separate ledger state.
They coordinate through the Super Validator.
Payments require Canton Wallet integration and Splice dependencies.

This four-party model shapes the project. The Splice container runs all three validators with configs in ``docker/modules/localnet/conf/splice/`` (app-provider/, app-user/, sv/), provider and user application modules in ``backend/`` and ``frontend/``, payment contracts in ``daml/``, and Splice DARs in ``daml/dars/``.

Work through the :ref:`quickstart-explore-the-demo` guide for the complete workflow walkthrough.

Development environment (Nix + Direnv)
---------------------------------------

The repository uses Nix and Direnv to provide consistent, cross-platform development dependencies including JDK, Node.js, and TypeScript.
If you prefer not to use Nix, you can work directly in ``quickstart/`` but will need to manage dependencies manually.
Review the `Canton Utility Setup <https://docs.digitalasset.com/utilities/devnet/setup/index.html#canton-utility-setup>`__ if you require utility deployment support.

**Key files:**

* ``.envrc`` - Activates Nix environment via Direnv
* ``nix/shell.nix`` - Defines development dependencies
* ``nix/sources.json`` - Pins Nix release for reproducible builds
* ``quickstart/`` - The main project directory

Quickstart directory structure
===============================

The ``quickstart/`` files and directories fall into one of three categories:

::

    Build Configuration
    - Makefile                  # Project orchestration
    - build.gradle.kts          # Root build configuration
    - buildSrc/                 # Custom Gradle plugins
    - gradle/                   # Gradle wrapper files
    - gradlew                   # Gradle wrapper (Unix)
    - gradlew.bat               # Gradle wrapper (Windows)
    - settings.gradle.kts       # Project structure definition
    
    Deployment Configuration
    - .env                      # Environment variables
    - compose.yaml              # Docker Compose configuration
    - config/                   # Service configurations
    - docker/                   # Docker image definitions
    
    Application Source
    - daml/                     # Smart contracts
    - backend/                  # Java backend services
    - frontend/                 # React frontend
    - common/                   # Shared API definitions

Build system
============

Gradle
------

Gradle builds the Java backend and Daml contracts.
The backend uses Transcode-generated classes from DAR files to interact with the Ledger API.

**Custom Gradle plugins** (``buildSrc/src/main/kotlin/``):

.. list-table::
   :header-rows: 1
   :widths: 40 80
   :align: left

   * - Plugin
     - Purpose
   * - ``ConfigureProfilesTask.kt``
     - Interactive generation of ``.env.local``
   * - ``Dependencies.kt``
     - Propagates ``.env`` versions to Gradle
   * - ``UnpackTarGzTask.kt``
     - Unpacks ``.tgz`` with symlink support
   * - ``VersionFiles.kt``
     - Reads ``.env`` and ``daml.yaml`` files

Make
----

Make provides a command-line interface to build tools and Docker Compose.
Run ``make help`` to see available commands.

**Common targets:**

* ``make setup`` - Configure deployment profile
* ``make build`` - Build all components
* ``make start`` - Start the application
* ``make status`` - Show running containers
* ``make stop`` - Stop the application

The ``Makefile`` serves as both executable commands and documentation of the development workflow.

Deployment configuration
========================

Docker Compose
--------------

Docker Compose orchestrates the local development environment, LocalNet, which simulates a Canton Network on your laptop.
It includes validator nodes, a super validator, Canton Coin wallet, and supporting services.

**Key files:**

* ``compose.yaml`` - Main Docker Compose configuration
* ``.env`` - Environment variables for all services
* ``config/`` - Service-specific configuration files
* ``docker/`` - Docker image build contexts

Port mapping
------------

LocalNet uses a prefix-suffix pattern for port numbers:

**Prefixes:**

* ``2xxx`` - Application User validator
* ``3xxx`` - Application Provider validator
* ``4xxx`` - Super Validator

**Common Suffixes:**

* ``x901`` - Ledger API
* ``x902`` - Admin API
* ``x903`` - Validator API
* ``x975`` - JSON API
* ``5432`` - PostgreSQL

**Examples**

* Application User Ledger API: ``2901``
* Provider Validator API: ``3903``
* Application User JSON API: ``2975``.

Port mapping security
---------------------

Port mappings for ``LocalNet`` expose the ``AdminAPI`` and ``Postgres`` ports, which is a security risk on a public network.
However, it's useful to have direct access to these ports when developing and testing locally.
**Do NOT** expose these ports when preparing configurations for non-local deployments.
You can remove ports in their appropriate Docker file.

Health checks
-------------

Health check endpoints for each validator are in ``..docker/splice/health-check.sh``.

.. code-block:: bash

    curl -f http://localhost:2903/api/validator/readyz  # App User
    curl -f http://localhost:3903/api/validator/readyz  # App Provider
    curl -f http://localhost:4903/api/validator/readyz  # Super Validator

Empty responses indicate healthy services.

Admin ports are defined in ``quickstart/docker/modules/localnet/compose.yaml``

.. code-block:: bash

    curl -v http://localhost:2902/admin    # Accesses App User admin if exposed
    curl -v http://localhost:3902/admin    # Accesses App Provider admin if exposed

See :ref:`quickstart-json-ledger-api` for detailed port usage and authentication patterns.

Application structure
=====================

Canton applications have three layers:

1. **User Interface** (frontend/) - React web application
2. **Local Business Logic** (backend/) - Java services, PQS queries, integrations
3. **Consensus Business Logic** (daml/) - Smart contracts requiring multi-party agreement

The Quickstart uses a fully mediated architecture where the backend handles all ledger interactions.
Alternatively, you could use a CQRS architecture where the frontend submits commands directly to the ledger and designate the backend to handle queries.

Daml smart contracts
---------------------

The licensing application demonstrates multi-party workflows requiring consensus between the app provider, user, and DSO (for payments).

::

    licensing/
    └── daml/
        └── Licensing/
            ├── AppInstall.daml     # User onboarding
            ├── License.daml        # License management
            └── Util.daml           # Helper functions

Core business flow
~~~~~~~~~~~~~~~~~~

The consensus layer handles multi-party agreements through these Daml templates:

**User Onboarding** (``AppInstall.daml``):

* ``AppInstallRequest`` - User initiates installation using the Propose/Accept pattern

  * Choices: ``AppInstallRequest_Accept``, ``AppInstallRequest_Reject``, ``AppInstallRequest_Cancel``

* ``AppInstall`` - Active installation relationship between provider and user

  * Choice: ``AppInstall_CreateLicense`` - Provider creates licenses for the user

**License Management** (``License.daml``):

* ``License`` - Time-based access control with expiration date

  * Choice: ``License_Renew`` - Creates ``AppPaymentRequest`` (Splice Wallet) and ``LicenseRenewalRequest``
  * Choice: ``License_Expire`` - Archives expired licenses

* ``LicenseRenewalRequest`` - Handles license extensions through Canton Coin payments

Why consensus layer?
~~~~~~~~~~~~~~~~~~~~

These operations require consensus because they involve agreements between multiple parties, making them unsuitable for local backend services.

1. **User creates** ``AppInstallRequest`` → Provider must see and respond
2. **Provider exercises** ``AppInstallRequest_Accept`` → Both parties must agree to create ``AppInstall``
3. **Provider creates** ``License`` **contracts** → User must accept terms
4. **License renewal** → Requires payment validation across user, provider, and DSO's payment system

Backend services
----------------

The backend is a Spring Boot application that mediates ALL ledger interactions using two distinct paths:

1. **Queries** → PQS (Participant Query Store) for fast read access to ledger state
2. **Commands** → Ledger API GRPC for exercising choices and creating contracts

This fully mediated architecture centralizes authentication and ledger access, keeping the frontend simple.

**Module structure** (``backend/src/main/java/com/digitalasset/quickstart/``):

.. list-table::
   :header-rows: 1
   :widths: 20 40 50
   :align: left

   * - Module
     - Purpose
     - Key Components
   * - ``security/``
     - OAuth2 authentication and access control
     - Bearer token validation
   * - ``service/``
     - OpenAPI endpoint implementations
     - Combines PQS queries with Ledger API commands
   * - ``ledger/``
     - Ledger API GRPC client
     - ``LedgerApi`` submits commands to validator
   * - ``repository/``
     - Business-logic PQS queries
     - ``DamlRepository`` provides domain-specific queries
   * - ``pqs/``
     - Low-level PQS access
     - ``Pqs`` generates SQL, queries PostgreSQL
   * - ``utility/``
     - Codegen and JSON utilities
     - ``DamlCodeGen`` accesses Transcode-generated Java classes from DARs
   * - ``config/``
     - Spring Boot configuration
     - ``@ConfigurationProperties`` components

Backend architecture pattern
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The backend provides two types of HTTP endpoints:

* **GET** - Query contracts and their state (via PQS)
* **POST** - Execute choices on contracts (via Ledger API, with contract IDs in URLs)

The backend uses Transcode-generated Java classes from DAR files to provide type-safe ledger interactions.
Rebuild the backend with ``make build`` to regenerate these classes after updating Daml contracts.

Frontend application architecture
----------------------------------

The frontend is a React application written in TypeScript using Vite for builds and Axios for HTTP transport.

The backend handles ALL ledger interactions.
The frontend never talks directly to Canton or the Ledger API.
This approach:

* Centralizes authentication and access control in one place
* Allows the frontend to integrate non-ledger data sources easily
* Uses OpenAPI schemas as data models (DTOs) between frontend and backend
* HTTP client: Axios with OpenAPI client generation

Some Canton applications use CQRS architecture where the frontend submits commands directly to the Ledger API using Daml-generated TypeScript.
This tighter coupling works well for Daml-centric applications but requires the frontend to understand Canton concepts like party IDs and contract IDs.

Common API definition
---------------------

``common/openapi.yaml`` defines the HTTP interface between frontend and backend.
The API uses:

* **GET** ``/api/resource`` - Query contracts and state (via PQS)
* **POST** ``/api/contracts/{contractId}/exercise`` - Execute choices (via Ledger API)

The OpenAPI schema generates TypeScript types for the frontend and validates requests in the backend.

Configuration reference
=======================

Environment variables
---------------------

The ``.env`` file contains version numbers, feature flags, and default configurations.
Use ``.env.local`` for local overrides (not tracked in git).

Docker Compose modules
----------------------

LocalNet is built from modular Docker Compose layers:

* Base LocalNet infrastructure (from Splice)
* Authentication (Keycloak)
* Observability (Grafana, Prometheus, Loki)
* PQS (Participant Query Store)
* Application services

Development workflow
====================

Quick Start
-----------

.. code-block:: bash

    cd quickstart/
    make setup          # Configure deployment
    make build          # Build all components
    make start          # Start LocalNet
    make status         # Verify containers running

Iterative development
---------------------

.. code-block:: bash

    make build-daml      # Rebuild Daml contracts
    make build-backend   # Rebuild Java services
    make build-frontend  # Rebuild React app
    make restart         # Restart services with new code

Debugging and logs
------------------

.. code-block:: bash

    make capture-logs    # Start log capture (separate terminal)
    make shell           # Open Daml Shell for ledger queries

See :ref:`quickstart-debugging-and-troubleshooting-lnav` for log analysis techniques.

Next steps
==========

Once you understand the project structure, visit the `TL;DR for new Canton Network developers <https://docs.digitalasset.com/build/3.4/overview/tldr.html>`__ for additional guides to explore.