# Canton Network Application Quickstart

This project provides scaffolding to develop a Canton Network application for the Global Synchronizer (CN GS). We intend that you clone the repository and incrementally update the solution to match your business operations. We assume that you have a Daml Enterprise license to leverage all of this project's features at runtime. However, an OSS developer can benefit from this project by understanding how a CN GS application is structured.

## Disclaimer

Once you are familiar with the QS, please review the technology choices and the application design to determine what changes are needed. Technology and design decisions are ultimately up to you. Please be aware that the CN QS is a rapidly evolving work in progress. 

## Docs and Guides

If you are impatient, then you can start by following the Engineer Setup below. Alternatively, you can peruse the documentation:
- [Quickstart Installation](docs/guide/CN-QS-Installation-20250314.pdf)
- [Exploring The Demo](docs/guide/ExploringTheDemo-20250404.pdf)
- [Project Structure](docs/guide/ProjectStructureGuide-20250317.pdf)
- [FAQ](docs/guide/CN-QS-FAQ-20250404.pdf)
- [Observability and Troubleshooting Overview](docs/guide/ObservabilityTroubleshootingOverview-20250220.pdf)

### Technical Documentation

- [Observability](docs/user/001-observability.md)
- [Topology](docs/user/002-topology.md)

Additional documentation and updates are planned weekly.

This project will be rapidly enhanced, so please check back often for updates.

## Engineer Setup

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

### Artifactory Access

Some artifacts are located in Digital Asset's [artifactory](https://digitalasset.jfrog.io). To access these artifacts the build system in this repository will use a `~/.netrc` file. You can get (or create) the necessary credentials on your [user profile](https://digitalasset.jfrog.io/ui/user_profile) page. The `.netrc` file should contain the following:

```sh
machine digitalasset.jfrog.io
login <username>
password <password>
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

In `DevNet` mode, you can configure a non-default `SPONSOR_SV_ADDRESS`, `SCAN_ADDRESS` and `ONBOARDING_SECRET_URL` or `ONBOARDING_SECRET` in the `quickstart/.env` file.

**Note**: Access to the Super Validator endpoints on DevNet may require a VPN setup.

## Available Make Targets

Run `make help` to see a list of all available targets, including (but not limited to):

- **start**: Builds and starts the application, including frontend and backend services, using Docker Compose. Also starts `observability` and/or `LocalNet` stack depending on configuration.
- **setup**: Configure the local development environment (enable DevNet/LocalNet, Observability)
- **stop**: Stops the application services, as well as the observability stack.
- **stop-application**: Like `stop`, but leaves the observability services running.
- **restart**: Re-runs the application services by stopping and then starting it again.
- **build**: Builds frontend, Daml model, and backend.
- **console-app-provider**: Starts the Canton console using Docker, connected to the running app provider ledger.
- **console-app-user**: Starts the Canton console using Docker, connected to the running app user ledger.
- **shell**: Starts Daml Shell using Docker, connected to the running application PQS database.
- **status**: Shows the status of Docker containers.
- **logs**: Shows logs of Docker containers.
- **tail**: Tails logs of Docker containers in real-time.
- **clean**: Cleans the build artifacts.
- **clean-docker**: Stops and removes Docker containers and volumes.
- **clean-application**: Like `clean-docker`, but leaves the observability services.
- **clean-all**: Stops and removes all Docker containers and volumes, including observability services.

## Topology

This diagram summarizes the relationship of services that are started as part of `make start`. The focus of `Canton Network Quickstart` is to provide a development environment for App Providers.

![QS Topology](docs/images/qs-topology.drawio.png)

For more information and detailed diagrams, please refer to the [Topology](docs/user/002-topology.md) documentation.

## Accessing Frontends

After starting the application with `make start` you can access the following UIs:

### Application UIs

- **Application User Frontend**
  - **URL**: [http://app-provider.localhost:3000](http://app-provider.localhost:3000)
  - **Description**: The main web interface of the application.

- **App User (`Org1`) Wallet UI**
  - **URL**: [http://wallet.localhost:2000](http://wallet.localhost:2000)
  - **Description**: Interface for managing user wallets.

- **App Provider Wallet UI**
  - **URL**: [http://wallet.localhost:3000](http://wallet.localhost:3000)
  - **Description**: Interface for managing user wallets.

### Super Validator UIs (if LocalNet enabled via `make setup`)

> **Note**: These interfaces are only accessible when starting in **LocalNet** mode. Run `make setup` to switch between `LocalNet` and `DevNet`.

- **Super Validator Web UI**
  - **URL**: [http://sv.localhost:4000](http://sv.localhost:4000)
  - **Description**: Interface for super validator functionalities.

- **Scan Web UI**
  - **URL**: [http://scan.localhost:4000](http://scan.localhost:4000)
  - **Description**: Interface to monitor transactions.

  > **Note**: `LocalNet` rounds may take up to 6 rounds (equivalent to one hour) to display in the scan UI.

All the Super Validator UIs are accessible via a gateway at [http://localhost:4000](http://localhost:4000).

The `*.localhost` domains will resolve to your local host IP `127.0.0.1`.

### Auth

To perform operations such as creating, editing, and archiving assets, users must be authenticated and authorized. The endpoints that perform these operations are protected by OAuth2 Authorization Code Grant Flow. GRPC communication between the backend service and participant is secured by OAuth2 Client Credentials Flow.

### Port Mappings

The LocalNet configuration includes port mappings for local development that should not be exposed in production deployments.
See the [Project Structure](docs/guide/ProjectStructureGuide-20250317.pdf) for more details.

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
