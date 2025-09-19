Quickstart top-level directory structure
========================================

Quickstart project directory
----------------------------

The files and directories in the CN Quickstart project directory fall into one of three categories:

::

  -  Build configuration

    - Makefile
    - build.gradle.kts
    - buildSrc/
    - gradle/
    - gradlew
    - gradlew.bat
    - settings.gradle.kts

  -  Deployment configuration

    - .env
    - compose.yaml
    - config/
    - docker/

  -  Application source

    - backend/
    - common/
    - daml/
    - frontend/

Build configuration
-------------------

The primary build tool used by the example project is Gradle.  
This is managed via the Gradle wrappers ``gradlew`` and ``gradlew.bat``. 
Gradle is used for the Java-based web services in `backend/` and to build Daml smart contracts via a simple wrapper that calls the Daml Assistant.

The backend takes advantage of classes generated from the Daml model to simplify interactions with the Ledger API. 
These are generated directly from the DAR files using the Transcode code generator.
The Gradle plugin to run the generator is part of the Transcode package, 
and is incorporated into the build process in ``daml/build.gradle.kts``.

``buildSrc/`` contains some custom Gradle plugins in ``buildSrc/src/main/kotlin/``:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - File
     - Description
   * - `ConfigureProfilesTask.kt`
     - Interactive generation of `.env.local` file for the project.
   * - `Credentials.kt`
     - Allows access to credentials stored in `~/.netrc`.
   * - `Dependencies.kt`
     - Propagates version config from `.env` to Gradle.
   * - `Repositories.kt`
     - Adds `digitalasset.jfrog.io` to the Maven artifact repositories.
   * - `UnpackTarGzTask.kt`
     - Provides (required) symlink support for unpacking `.tgz` files.
   * - `VersionFiles.kt`
     - Provides access to `.env` files and `daml.yaml` files from Gradle.

The project also uses `Make <https://www.oreilly.com/openbook/make3/book/index.csp>`__ as a project choreographer, providing a convenient command-line interface to the various scripts and build tools as well as docker-compose commands. 
This is similar to the common practice of defining aliases for common dev-loop tasks.
`Make <https://en.wikipedia.org/wiki/Make_(software)>`__ documents and shares these tasks under revision control.
Use ``make help`` to view the currently supported tasks.
The ``Makefile`` itself is intended to be implicit documentation of how each of these steps is performed. 
By default, ``make`` also prints any commands it executes to ``stdout`` and this can also help familiarize new developers with how the dev-loop is structured. 
See the Makefile for current Make features.

Deployment configuration
------------------------

Local deployment is handled via `Docker <https://docs.docker.com/>`__ and `Docker Compose <https://docs.docker.com/compose/>`__ that constructs a `LocalNet` on your laptop. 

Each Docker image has its own colocated environment variables.

``compose.yaml`` is the top-level Docker Compose configuration file.

``config/`` contains all the various service configuration files required by the various docker containers.

``docker/`` contains the various docker image configurations.

Port mappings
-------------

A number of ports are configured to be exposed to localhost.
These ports are configured using a prefix|suffix arrangement.

A single-digit prefix identifies the “entity” associated with the relevant node, 
and the suffix is the usual four-digit port number associated with the relevant service.

.. list-table:: LocalNet Port Prefixes
   :widths: 30 70
   :header-rows: 1

   * - Prefix
     - Entity
   * - `2${PORT}`
     - Application User
   * - `3${PORT}`
     - Application Provider
   * - `4${PORT}`
     - Super Validator

``LocalNet`` port suffixes are as follows:

.. list-table:: LocalNet Port Suffixes
   :widths: 30 70
   :header-rows: 1

   * - Suffix
     - Service
   * - `901`
     - Participant Ledger API Port
   * - `902`
     - Participant Admin API Port
   * - `903`
     - Validator Admin API Port
   * - `975`
     - Participant JSON API Port
   * - `5432`
     - Postgres Port

For example, the ``JSON API Port`` for the Application User is ``2975``,
while the ``Ledger API Port`` for the Super Validator is ``2901``.
Splice ``LocalNet`` definitions are published in the `Splice docs <https://docs.dev.sync.global/app_dev/testing/localnet.html#exposed-ports>`__.

Port mappings security
~~~~~~~~~~~~~~~~~~~~~~

The port mappings for ``LocalNet`` expose the ``AdminAPI`` port and the ``Postgres`` port, 
both of which would normally be a security risk. 
However, having direct access to these ports when running on a local developer's machine can be useful. 
**These ports should not be exposed when preparing deployment configurations for non-local deployments.**

The port suffixes are defined as environment variables.
For any port mappings you wish to disable, you can find and remove the relevant Docker ``port``: entry in the appropriate file.
To name a few ports, the default setup exposes:

- **Ledger API ports** (2901, 3901, 4901): Canton Ledger API access
- **Admin API ports** (2902, 3902, 4902): system administration
- **Validator API ports** (2903, 3903, 4903): status monitoring
- **JSON API ports** (2975, 3975, 4975): Daml ops and smart contract deployment

**Health checks**
You can find the health check endpoints for each validator in ``quickstart/docker/modules/localnet/docker/splice/health-check.sh``.
Empty responses indicate healthy services.

.. code-block:: bash

   curl -f http://localhost:2903/api/validator/readyz  # App User
   curl -f http://localhost:3903/api/validator/readyz  # App Provider  
   curl -f http://localhost:4903/api/validator/readyz  # Super Validator

**Access admin ports**  
Admin ports are defined in ``quickstart/docker/modules/localnet/compose.yaml``

.. code-block:: bash

   curl -v http://localhost:2902/admin    # Would access App User admin if exposed
   curl -v http://localhost:3902/admin    # Would access App Provider admin if exposed

**Upload DAR via JSON API with Authentication token**  
These endpoints are also defined in ``compose.yaml``.

.. code-block:: bash

   # Load environment variables (run from quickstart directory)
   cd quickstart
   set -a
   source docker/modules/keycloak/env/app-user/on/oauth2.env
   source docker/modules/keycloak/compose.env
   set +a
   
   # Use the actual token URL from environment, but replace docker hostname with localhost
   TOKEN_URL=$(echo "$AUTH_APP_USER_TOKEN_URL" | sed 's/nginx-keycloak/localhost/')
   echo "Using token URL: $TOKEN_URL"
   
   # Get OAuth2 token using environment variables and URL
   TOKEN=$(curl -fsS "$TOKEN_URL" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "client_id=$AUTH_APP_USER_VALIDATOR_CLIENT_ID" \
     -d "client_secret=$AUTH_APP_USER_VALIDATOR_CLIENT_SECRET" \
     -d "grant_type=client_credentials" \
     -d "scope=openid" | jq -r .access_token)
   
   echo "Token obtained: ${TOKEN:0:20}..."
   
   # Upload DAR if token is valid
   if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
     curl -v -X POST http://localhost:2975/v2/packages \
       -H "Content-Type: application/octet-stream" \
       -H "Authorization: Bearer $TOKEN" \
       --data-binary @./daml/licensing/.daml/dist/quickstart-licensing-0.0.1.dar
   else
     echo "Failed to get authentication token"
   fi

Application source
------------------

As with most Daml applications, the source code falls into four categories:

.. list-table:: Application directories
   :widths: 20 30 50
   :header-rows: 1

   * - Directory
     - Tech Stack
     - Contents
   * - `daml/`
     - Daml
     - The Daml model and DAR dependencies
   * - `frontend/`
     - React, Vite, Axios, Typescript
     - Web front end code
   * - `backend/`
     - Java, Springboot, Protobuf
     - Back end code. Currently PQS backed OpenAPI endpoints for the front end, automations, integrations, and other off-ledger components.
   * - `common/`
     - OpenAPI
     - Interface definitions shared by one or more of the previous three categories.
       Currently an openapi.yaml file defining the interface between Front and Back ends.

The frontend and backend examples can be written using any relevant technology stack. 
The backend may be written using Node.js, C#, or any other language. 
The Daml codegen tooling supports Java, JavaScript, and TypeScript which has driven the choice of stack for the example application.
