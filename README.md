# Canton Network application quickstart

This project provides scaffolding to develop a Canton Network application for the Global Synchronizer (CN GS). We intend that you clone the repository and incrementally update the solution to match your business operations. We assume that you have a Daml Enterprise license to leverage all of this project's features at runtime. However, an OSS developer can benefit from this project by understanding how a CN GS application is structured.
 
To run the Quickstart you need some binaries from Artifactory.  Request Artifactory access by clicking [here](https://www2.digitalasset.com/contact-us-access-to-jfrog) and we will get right back to you.  
The terms and conditions for the binaries can be found [here](https://github.com/digital-asset/cn-quickstart/blob/main/terms.md). 

The is licensed under the BSD Zero Clause License.

## Disclaimer

Once you are familiar with the QS, please review the technology choices and the application design to determine what changes are needed. Technology and design decisions are ultimately up to you. Please be aware that the CN QS is a rapidly evolving work in progress. 

## Docs and guides

If you are impatient, then you can start by following the Engineer Setup below. Alternatively, you can peruse the documentation:
- [Quickstart Installation](docs/guide/CN-QS-Installation-20250516.pdf)
- [Exploring The Demo](docs/guide/ExploringTheDemo-20250516.pdf)
- [Project Structure](docs/guide/ProjectStructureGuide-20250317.pdf)
- [FAQ](docs/guide/CN-QS-FAQ-20250516.pdf)
- [Observability and Troubleshooting Overview](docs/guide/ObservabilityTroubleshootingOverview-20250220.pdf)

### Technical documentation

- [Observability](sdk/docs/user/001-observability.md)
- [Topology](sdk/docs/user/002-topology.md)

Additional documentation and updates are planned weekly.

This project will be rapidly enhanced, so please check back often for updates.

## Engineer setup

This repository uses `direnv`, `nix`, and `docker-compose` to provide development dependencies:

* how to [install direnv](https://direnv.net/docs/installation.html)
* how to [install nix](https://nix.dev/install-nix.html)
* how to [install docker-compose](https://docs.docker.com/compose/install/) 

**Important (MacOS only):** Run the following command to download and install the Daml SDK with the correct version:
```sh
cd quickstart
make install-daml-sdk
```

Project files are located in the `quickstart` directory. You can use the `quickstart` directory as a standalone project without nix, but you will need to provide binary dependencies manually.

### Artifactory access

As mentioned, Some Docker images are from Digital Asset's [artifactory](https://digitalasset.jfrog.io). To access these artifacts the build system in this repository uses a `~/.netrc` file. You can get (or create) the necessary credentials on your [user profile](https://digitalasset.jfrog.io/ui/user_profile) page. The `.netrc` file should contain the following:

```sh
machine digitalasset.jfrog.io
login <username>
password <identity_token>
```

**Additionally,** to pull licensed docker images you must also log into the following Docker registries:

```bash
docker login -u <username> -p <password> digitalasset-docker.jfrog.io
```

Use the same username and password from your Artifactory credentials.

## Quickstart

To start the application:

```bash
# In the local repository directory
$ direnv allow
$ cd quickstart

# Setup your quickstart environment
$ make setup

# Build the application
$ make build

# Start the application, Canton services, and Observability (if enabled)
$ make start

# In a separate shell - run a Canton Console for the App Provider
$ make console-app-provider

# In a separate shell - run Daml Shell
$ make shell
```

If containers fail to start, ensure docker compose is configured to allocate enough memory (recommended minimum total of 32gb).

When running `make start` for the first time, an assistant will help you setting up the local deployment. You can choose to run the application in `DevNet` or `LocalNet` mode (recommended) for local development and testing, the latter meaning that a transient Super Validator is set up locally. You can change this later by running `make setup`.

**Note**: Access to the Super Validator endpoints on DevNet may require a VPN setup.

## Available make targets

Run `make help` to see a list of all available targets, including (but not limited to):

- **start**: Builds and starts the application, including frontend and backend services, using Docker Compose. Also starts `observability` and/or `LocalNet` stack depending on configuration.
- **setup**: Configure the local development environment (enable DevNet/LocalNet, Observability)
- **stop**: Stops the application services, as well as the observability stack.
- **stop-application**: Like `stop`, but leaves the observability services running.
- **restart**: Re-runs the application services by stopping and then starting it again.
- **build**: Builds frontend, Daml model, and backend.
- **compose-config**: Displays the finalized configuration for each service initiated by the `make start` command. Please note that dynamic environment variables, such as `APP_PROVIDER_PARTY`, which are resolved at runtime, are not included in this output.
- **canton-console**: Starts the Canton console using Docker, connected to the running app provider, app-user, sv ledgers.
- **shell**: Starts Daml Shell using Docker, connected to the running application PQS database.
- **status**: Shows the status of Docker containers.
- **capture-logs**: Consumes Docker events and starts capturing logs to `/logs` directory for each service when `start` Docker event is observed. Ideal for diagnostic purposes. 
- **logs**: Shows logs of Docker containers.
- **tail**: Tails logs of Docker containers in real-time.
- **clean**: Cleans the build artifacts.
- **clean-docker**: Stops and removes Docker containers and volumes.
- **clean-application**: Like `clean-docker`, but leaves the observability services.
- **clean-all**: Stops and removes all Docker containers and volumes, including observability services.

## Topology

Quickstart is built on top of https://github.com/hyperledger-labs/splice/tree/main/cluster/compose/localnet. Check [documentation](https://docs.sync.global/app_dev/testing/localnet.html) for more information about Splice LocalNet.

This diagram summarizes the relationship of services that are started as part of `make start`. The `canton` and `splice` services are configured to serve multiple logically separate components (each component represented with a box in the diagram) from a single container to reduce resource consumption. Similarly the `postgres` service contains multiple databases required by QS services. One `nginx` service is used as proxy for all QS services that needs one except for `keycloak` that has its own `nginx-keycloak` as it needs to be ready before other services start. The focus of `Canton Network Quickstart` is to provide a development environment for App Providers.

![QS Topology](sdk/docs/images/qs-topology.drawio.png)

For more information and detailed diagrams, please refer to the [Topology](sdk/docs/user/002-topology.md) documentation.

## Accessing frontends

After starting the application with `make start` you can access the following UIs:

### Application UIs

- **App User ANS UI**
    - **URL**: [http://ans.localhost:2000](http://ans.localhost:2000)
    - **Description**: Interface for registering names.

- **App Provider ANS UI**
    - **URL**: [http://ans.localhost:3000](http://ans.localhost:3000)
    - **Description**: Interface for registering names.

- **Application user frontend**
  - **URL**: [http://app-provider.localhost:3000](http://app-provider.localhost:3000)
  - **Description**: The main web interface of the application.

- **App user wallet UI**
  - **URL**: [http://wallet.localhost:2000](http://wallet.localhost:2000)
  - **Description**: Interface for managing user wallets.

- **App provider wallet UI**
  - **URL**: [http://wallet.localhost:3000](http://wallet.localhost:3000)
  - **Description**: Interface for managing user wallets.

### Super Validator UIs (if LocalNet enabled via `make setup`)

> **Note**: These interfaces are only accessible when starting in **LocalNet** mode. Run `make setup` to switch between `LocalNet` and `DevNet`.

- **Super Validator web UI**
  - **URL**: [http://sv.localhost:4000](http://sv.localhost:4000)
  - **Description**: Interface for super validator functionalities.

- **Scan Web UI**
  - **URL**: [http://scan.localhost:4000](http://scan.localhost:4000)
  - **Description**: Interface to monitor transactions.

  > **Note**: `LocalNet` rounds may take up to 6 rounds (equivalent to one hour) to display in the scan UI.

The `*.localhost` domains will resolve to your local host IP `127.0.0.1`.

## Exploring Quickstart Docker Compose

Before exploring advanced topics, we recommend familiarizing yourself with the core components of the Licensing Model Workflow within Quickstart. In particular, begin by reviewing the implementation of the `backend-service`, which serves as an excellent entry point.

If you have already explored the Quickstart web UI and would now like to understand how the Quickstart Docker Compose configuration is orchestrated, start by running a simple setup using `make setup` with Observability and OAuth2 disabled. Then, execute the following command to inspect the resolved configuration for the backend service:

```bash
make compose-config | tail -n +2 | yq eval '.services.backend-service'
```

This command outputs a configuration similar to the example below:

```yaml
command:
  - /app/start.sh
container_name: backend-service
depends_on:
  pqs-app-provider:
    condition: service_started
    required: true
  splice-onboarding:
    condition: service_healthy
    required: true
environment:
  _JAVA_OPTIONS: -XX:-UseCompressedOops -Xms512m -Xmx700m
  AUTH_APP_PROVIDER_BACKEND_USER_NAME: app-provider-backend
  BACKEND_PORT: "8080"
  LEDGER_HOST: canton
  LEDGER_PORT: "3901"
  POSTGRES_DATABASE: pqs-app-provider
  POSTGRES_HOST: postgres
  POSTGRES_PASSWORD: supersafe
  POSTGRES_PORT: "5432"
  POSTGRES_USERNAME: cnadmin
  SPRING_PROFILES_ACTIVE: shared-secret
  VALIDATOR_URI: http://splice:3903/api/validator
image: eclipse-temurin:17.0.12_7-jdk
labels:
  description: 'Backend service supporting the Quickstart Licensing workflow. Note: The APP_PROVIDER_PARTY environment variable is dynamically resolved at runtime before the main process is initiated.'
mem_limit: "1073741824"
networks:
  default: null
ports:
  - mode: ingress
    target: 8080
    published: "8080"
    protocol: tcp
volumes:
  - type: bind
    source: /repositories/cn-quickstart/quickstart/backend/build/distributions/backend.tar
    target: /backend.tar
    bind:
      create_host_path: true
  - type: bind
    source: /repositories/cn-quickstart/quickstart/docker/backend-service/start.sh
    target: /app/start.sh
    bind:
      create_host_path: true
  - type: volume
    source: onboarding
    target: /onboarding
    volume: {}
working_dir: /app
```

This configuration demonstrates how the `backend-service` relies on the Quickstart-provided infrastructure. Quickstart automates much of the local environment setup for LocalNet, allowing you to prioritize application development. As you progress toward deployment and explore cloud orchestration, a deeper grasp of service configuration is invaluable. For now, consider these services a ready-to-use infrastructure foundation.

Then explore `register-app-user-tenant`, the service that registers AppUser tenants to the `backend-service`. That allows end users from the AppUser organization to log in and Quickstart the web UI. That, in turn, ties the AppUser Identity Provider to the AppUser primary party ID. That means that if the end user is logged in through this Identity Provider, the user can then act as the AppUser primary party. The `register-app-user-tenant` service utilizes functionality provided by the `splice-onboarding` module to make the task as simple as possible. 

This step can also be performed manually through the web UI if you log in to Quickstart as `app-provider` and navigate to the tenants tab. At that tab, you can also see a list of registered tenants and verify that the `AppUser` tenant was automatically pre-registered for you by `register-app-user-tenant`.

Once you run `make create-app-install-request`, the `docker/create-app-install-request` service executes a script that initiates the Licensing workflow on behalf of the `app-user`. This script leverages the capabilities of the `splice-onboarding` module to streamline the process. In a production environment, the initial Licensing workflow step would be executed by submitting a command to the AppUser Participant Node, potentially supported by a dedicated web UI within the AppUser infrastructure.


### Authorization

Quickstart support to different authorization modes:
- **oauth2** (default)
- **shared-secret**
See Splice LocalNet documentation for the shared-secret mode which is default Splice LocalNet auth mode.

#### OAuth2 mode - keycloak setup
To perform operations such as creating AppInstallRequest and renewing License, users must be authenticated and authorized. The endpoints that perform these operations are protected by OAuth2 Authorization Code Grant Flow. GRPC communication between the backend service and participant is secured by OAuth2 Client Credentials Flow.

In OAuth2 mode, Quickstart starts a local multi-tenant instance of [keycloak](https://www.keycloak.org/). 
 Two registered tenants are `AppProvider` and `AppUser`. Tenants have pre-configured users `app-provider` and `app-user`, as well as clients needed for validator, wallet, pqs, frontend, and backend service. Pre-configured users, clients, and realms are imported from the `docker/compose/modules/keycloak/conf/data` folder on Keycloak startup. The configuration in that folder is exported from the Keycloak instance after manual configuration via [Keycloak Administration Console](http://keycloak.localhost:8082/admin/master/console/) by running commands
```
/opt/keycloak/bin/kc.sh export --dir=/opt/keycloak/data/import --realm AppUser --optimized
/opt/keycloak/bin/kc.sh export --dir=/opt/keycloak/data/import --realm AppProvider --optimized
```
Pre-configured users, clients and realms are used directly in Quickstart components and via environment variables. Each component, module or backend-service refers to the pre-configured values in its environment variables. e.g. `docker/modules/keycloak/env/app-provider/on/oauth2.env`, `docker/backend-service/onboarding/env/oauth2.env`

#### Backend service tenant registration
Only the end users from an organization registered using endpoint `http://backend-service:${BACKEND_PORT}/admin/tenant-registrations` are allowed to log into the Quickstart web-ui. `AppUser` organization is registered on Quickstart startup by calling registration script in `register-app-user-tenant` docker container.

### Port mappings

You can find the port mappings scheme in the Splice LocalNet [documentation](https://docs.sync.global/app_dev/testing/localnet.html).
See the [Project structure](sdk/docs/guide/ProjectStructureGuide-20250317.pdf) for more details.

## Docker Compose-Based Development for LocalNet
The Quickstart leverages Docker Compose for modular development. Instead of relying on a single extensive docker-compose.yaml file, this approach orchestrates multiple compose files and corresponding environment files for each Quickstart module. Splice LocalNet is housed within the `docker/modules/localnet` directory. In the `Makefile`, Docker Compose commands are dynamically assembled from Splice LocalNet, Quickstart modules, and Quickstart-specific compose and environment files, arranged in an order that respects the interdependencies of the various components. 

Some modules (e.g., Keycloak and Observability) are optional and can be toggled on or off based on the selections made during `make setup`. When the Docker Compose command is executed, all specified Compose YAML files are merged in the order they appear on the command line. Likewise, the environment is built by applying each environment file in sequence; if the same variable is defined in multiple files, the value from the later file will overwrite the previous ones.
 
The `splice-onboarding` module supports two distinct operational modes. Initially, it performs a one-time setup procedure for Canton, Splice and modules. This initialization includes creating a ledger user and assigning necessary permissions. Developers can customize this process by specifying DAR files (and mounting it to file in `/canton/dars` in `splice-onboarding`) for ledger upload, custom shell scripts, or environment variables through their project’s `compose.yaml` file. For example:

```yaml
splice-onboarding:
  env_file:
    - ./docker/backend-service/onboarding/env/${AUTH_MODE}.env
  volumes:
    - ./docker/backend-service/onboarding/onboarding.sh:/app/scripts/on/backend-service.sh
    - ./daml/licensing/.daml/dist/quickstart-licensing-0.0.1.dar:/canton/dars/quickstart-licensing-0.0.1.dar
```

Furthermore, developers may want to leverage the `splice-onboarding` module to execute custom onboarding scripts once all dependent services are operational (for instance, the `register-app-user-tenant` script) or to initialize specific workflows (such as scripts defined in `/docker/create-app-install-request/compose.yaml`).

By integrating this approach, developers can leverage prepopulated environment variables, such as `APP_PROVIDER_PARTY` and other authentication-related settings, while also accessing a suite of tools bundled with the `splice-onboarding` container. These tools, including utilities like curl, jq, and jwt-cli, together with an library of shell functions found in `docker/modules/splice-onboarding/docker/utils.sh` that demonstrate on how to utilize JSON Ledger API HTTP endpoints effectively. This comprehensive setup facilitates the achievement of necessary functionality with minimal additional configuration.

Utilizing Docker Compose’s [merge mechanism](https://docs.docker.com/compose/how-tos/multiple-compose-files/merge/), developers have complete control over the configuration. They can add any settings by providing a custom `compose.yaml` (which is usually the first file processed in the Docker Compose command) or by appending a `compose.override.yaml` file at the end to override default configurations defined by Splice LocalNet or Quickstart modules.

Please note that while Quickstart is designed to streamline local development, deploying to production requires additional considerations. In the Quickstart demo, the `splice-onboarding` component facilitates initialization, onboarding, and the execution of scripts that drive the demonstration workflows. However, this component is not intended for use in a production environment. In a production-grade environment, you would typically utilize an orchestration framework such as Kubernetes and replace certain automated configurations with controlled, manual configuration steps. This approach ensures enhanced security and clear separation of services in line with enterprise standards.

### Modules

The Quickstart repository includes several modular components that can be reused in developer projects outside of Quickstart. These modules are located in the `docker/modules` directory by default; however, they can be sourced from any directory by setting the `MODULES_DIR` environment variable accordingly. 

Splice LocalNet is a special module borrowed from the [Splice repository](https://github.com/hyperledger-labs/splice/tree/main/cluster/compose/localnet) and is placed by default in `docker/modules`. It can also be relocated by properly configuring the LOCALNET_DIR environment variable. 

Each module provides specific functionality and may depend on other modules. The currently supported modules are:

- **keycloak** (optional): Adds support for OAuth2 authorization to Splice LocalNet.
- **splice-onboarding**: Provides onboarding capabilities, including a collection of tools, resolved environment settings, and shell scripts for calling JSON Ledger API HTTP endpoints.
- **pqs**: Offers preconfigured PQS instances for Splice LocalNet participants.
- **observability**: Introduces observability infrastructure components.
- **daml-shell**: A standalone module that enables launching Daml Shell. By default, it connects to the pqs-app-provider’s Postgres database.

### Docker Profiles
Docker profiles are used in both Splice LocalNet and Quickstart to enable or disable specific functionalities. Each module can support multiple profiles. For example, Splice LocalNet defines the following five profiles:
- **app-provider**
- **app-user**
- **sv**
- **swagger-ui**
- **console**

The `console` module runs as a standalone container, while the other modules start by default unless explicitly disabled (e.g., by omitting the profile flag such as --profile app-provider). In some implementations, modules rely on environment variables to determine the active profiles. In these cases, you should set the corresponding environment variable—such as APP_PROVIDER_PROFILE—to either "on" or "off". This approach is necessary because Docker does not inherently expose profile configuration details within the Docker Compose file or inside the container environments.

### Environment

There are two distinct types of environment files:

- Files used primarily for Docker Compose configuration.
- Files intended for Docker container environment settings.

The first category includes files such as `.env`, `.env.local`, `${LOCALNET_DIR}/env/<dev|local>.env`, `${LOCALNET_DIR}/env/common.env`, and any `compose.env` files from the modules. The second category encompasses the remaining files found under `${LOCALNET_DIR}/env` as well as the `env` directories within the modules.

In the Docker container environment files, you can reference variables defined in the Docker Compose environment. Simply declare a variable as `VAR=${VAR}` to ensure that the value from the Docker Compose environment is available within the container.


### Dynamic Configuration

In certain situations, it is necessary to share runtime information between services. For instance, in the case of the `backend-service`, the environment variable `APP_PROVIDER_PARTY` is mandatory. In a production-like environment, this information would typically be provided manually by a system administrator. The party ID becomes available only after the complete initialization of Canton/Splice. To automate this process, one might consider retrieving the party ID by querying the JSON Ledger API HTTP endpoint; however, adding extra environment variables to support JWT token retrieval for this purpose could clutter the backend service configuration.

A preferable solution is to leverage the existing `splice-onboarding` service, which already possesses the appropriate environment and tools to perform this task. By mounting a custom script into the splice-onboarding container (for example, mapping `./docker/backend-service/onboarding/onboarding.sh` to `/app/scripts/on/backend-service.sh`), the splice-onboarding service executes the script (or any script located in the `/app/scripts/on/` directory) at the conclusion of its initialization routine.

Within the script, the acquired information can be shared with the backend service as follows:
```
  share_file "backend-service/on/backend-service.sh" <<EOF
  export APP_PROVIDER_PARTY=${APP_PROVIDER_PARTY}
EOF
```
In this context, `share_file` is a utility function that writes the provided content (the second argument) to the specified file (the first argument) on the shared volume `onboarding`. This volume is also mounted in the `backend-service`, and the startup script (docker/backend-service/start.sh) sources the newly shared script prior to executing the main command of the backend service, thereby ensuring that the `APP_PROVIDER_PARTY` environment variable is available to the service.


## License

**You may use the contents of this repository in parts or in whole according to the `0BSD` license.**

Copyright &copy; 2025 Digital Asset (Switzerland) GmbH and/or its affiliates

> Permission to use, copy, modify, and/or distribute this software for
> any purpose with or without fee is hereby granted.
> 
> THE SOFTWARE IS PROVIDED “AS IS” AND THE AUTHOR DISCLAIMS ALL
> WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES
> OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE
> FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY
> DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
> AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
> OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
