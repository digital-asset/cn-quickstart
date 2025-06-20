Quickstart toplevel directory structure
=======================================

Quickstart project directory
----------------------------

As is typical in a project directory the files and directories here fall
into one of three categories:

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

The primary build tool used by the example project is Gradle. As is
recommended, this is managed via the Gradle wrappers `gradlew` and
`gradlew.bat`. This is used for the Java-based web services in `backend/`.
It is also used to build Daml smart contracts via a simple wrapper that
calls the Daml Assistant [12]_.

The backend takes advantage of classes generated from the Daml model to
simplify interactions with the Ledger API. These are generated directly
from the DAR files using the Transcode code generator. The Gradle plugin
to run the generator is part of the Transcode package, and is
incorporated into the build process in `daml/build.gradle.kts`.

`buildSrc/` contains some custom Gradle plugins in `buildSrc/src/main/kotlin/`:

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

The project also uses Make [13]_ as a project choreographer, providing a
convenient command-line interface to the various scripts and build tools
as well as docker-compose commands. This is similar to the common
practice of defining aliases for common dev-loop tasks. Make has the
advantage of documenting and sharing these tasks under revision
control. [14]_ Use `make help` to view the currently supported tasks. The
`Makefile` itself is intended to be implicit documentation of how each of
these steps is performed. By default, `make` also prints any commands it
executes to `stdout` and this can also help familiarize new developers to
how the dev-loop is structured. If your team is unfamiliar with Make, at
the end of this guide [15]_, we have documented the Make features used
in the current Makefile with links to additional documentation.

Local deployment (LocalNet) configuration
-----------------------------------------

Local deployment is handled via Docker [16]_ and Docker Compose [17]_ in
the usual fashion. Like other blockchains, it constructs a `LocalNet` on
your laptop. In summary:

`.env` and `.env.local` define the necessary environment variables.

`compose.yaml` is the toplevel Docker Compose configuration file

`config/` contains all the various service configuration files required by
the various docker containers.

`docker/` contains the various docker image configurations.

LocalNet port mappings
----------------------

For convenience, the `LocalNet` configuration exposes a number of ports to
localhost. For ease of use, the ports are configured using a
prefix|suffix arrangement. A single-digit prefix is used to identify the
“entity” associated with the relevant node; and, the suffix is the usual
four-digit port number associated with the relevant service.

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

`LocalNet` port suffixes are as follows:

.. list-table:: LocalNet Port Suffixes
   :widths: 30 70
   :header-rows: 1

   * - Suffix
     - Service
   * - `5001`
     - Participant Ledger API Port
   * - `5002`
     - Participant Admin API Port
   * - `5003`
     - Validator Admin API Port
   * - `7575`
     - Participant JSON API Port
   * - `5432`
     - Postgres Port

So for example the `JSON API Port` for the Application User is `27575`;
while the `Ledger API Port` for the Super Validator is `25001`.

Important security note regarding port mappings
-----------------------------------------------

Be aware that the port mappings for `LocalNet` include exposing both the
`AdminAPI` port and the `Postgres` port, both of which would normally be a
security risk. Having direct access to these ports when running on a
local developers machine can be useful. **These ports should not be
exposed when preparing deployment configurations for non-local
deployments.**

Should you wish to disable these mappings even for the `LocalNet`
deployment, the port suffixes are defined as environment variables in
the `.env`. For any port mappings you wish to disable, you can find and
remove the relevant Docker `port`: entry in the `compose.yaml` file.

Application Source
------------------

As with most Daml applications the source code falls into four
categories:

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
     - Back end code. Currently PQS backed OpenAPI endpoints for the front end [18]_.
   * - `common/`
     - OpenAPI
     - Interface definitions shared by one or more of the previous three categories.
       Currently an openapi.yaml file defining the interface between Front and Back ends.

Both the frontend and backend examples can be written using any relevant
technology stack. In particular, there is no reason why the backend
could not be written using Node.js, C#, or any other language. As of the
time this was written, the Daml codegen tooling provided by Digital
Asset supports Java, Javascript, and Typescript which has driven the
choice of stack for the example application.

.. [12]
   This wrapper also contains convenience functions to download and install the correct version of the Daml SDK.

.. [13]
   https://www.oreilly.com/openbook/make3/book/index.csp

.. [14]
   The Makefile is written to be self-documenting, this includes autogenerating “usage” as a default help target

.. [15]
   `Canton Quickstart Project Structure <https://docs.google.com/document/d/1DsmvBBP5Ldlzq76bdVvH05UYQRRHLtu5zCEs-fIDAic/edit?tab=t.0#bookmark=id.ajegdjdt1k5e>`__
   Short Makefile Primer

.. [16]
   https://docs.docker.com/

.. [17]
   https://docs.docker.com/compose/

.. [18]
   This is also where you should expect to find any automation, integration, and other off-ledger components