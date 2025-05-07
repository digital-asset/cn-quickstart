==================
Exploring The Demo 
==================
---------------------------------------
Canton Network Quickstart Guide \| 2025
---------------------------------------

**Contents**

`Exploring the Demo <#exploring-the-demo>`__

`Prerequisites <#prerequisites>`__

`Walkthrough <#walkthrough>`__

   `Canton Console <#canton-console>`__

   `Daml Shell <#daml-shell>`__

   `Connect to DevNet <#connect-to-devnet>`__

   `Important: Migration ID for DevNet
   Connections <#important-migration-id-for-devnet-connections>`__

   `Configuring Non-Default DevNet
   Sponsors <#configuring-non-default-devnet-sponsors>`__

   `SV UIs <#sv-uis>`__

   `Canton Coin Scan <#canton-coin-scan>`__

   `Observability Dashboard <#observability-dashboard>`__

`Development Journey in the CN QS
Lifecycle <#development-journey-in-the-cn-qs-lifecycle>`__

   `CN QS Components <#cn-qs-components>`__

   `Development Tools <#development-tools>`__

   `LocalNet <#localnet>`__

   `Network Components <#network-components>`__

   `ScratchNet <#scratchnet>`__

   `Sample Application <#sample-application>`__

   `Application Components <#application-components>`__

`Development Lifecycle <#development-lifecycle>`__

   `Learning Phase <#learning-phase>`__

   `Experimentation Phase <#experimentation-phase>`__

   `Development Phase <#development-phase>`__

   `Gradle Settings <#gradle-settings>`__

   `Environment Variables <#environment-variables>`__

   `Docker Compose <#docker-compose>`__

   `Separation Phase <#separation-phase>`__

   `Ongoing Updates <#ongoing-updates>`__

`Upgrades On The Global
Synchronizer <#upgrades-on-the-global-synchronizer>`__

   `Type 1: Backward-Compatible
   Changes <#type-1-backward-compatible-changes>`__

   `Type 2: Daml Model Changes <#type-2-daml-model-changes>`__

   `Type 3: Non-Compatible Protocol
   Changes <#type-3-non-compatible-protocol-changes>`__

   `Preparing for Upgrades <#preparing-for-upgrades>`__

`Keycloak in the CN QS <#keycloak-in-the-cn-qs>`__

   `Realm Structure <#realm-structure>`__

   `Keycloak Configuration <#keycloak-configuration>`__

   `Customizing Keycloak for Business
   Needs <#customizing-keycloak-for-business-needs>`__

   `Accessing the Admin Console <#accessing-the-admin-console>`__

   `Customization Scenarios <#customization-scenarios>`__

   `Add a New User <#add-a-new-user>`__

   `Modify Client Settings <#modify-client-settings>`__

   `Add a New Client <#add-a-new-client>`__

   `Update Environment Variables <#update-environment-variables>`__

   `Troubleshooting <#troubleshooting>`__

`Next Steps <#next-steps>`__

Exploring the Demo
==================

The CN QS and its guides are a work-in-progress (WIP). As a result, the
CN QS guides may not be a little out of step with the application. If
you find errors or other inconsistencies, please contact your
representative at Digital Asset.

This section works through a complete business operation within the CN
QS.

Prerequisites
=============

You should have successfully `installed the CN
QS <https://github.com/digital-asset/cn-quickstart/blob/main/docs/guide/CN-QS-Installation-20250314.pdf>`__
before beginning this demonstration.

Access to the `CN-Quickstart Github
repository <https://github.com/digital-asset/cn-quickstart>`__ and `CN
Docker
repository <https://digitalasset.jfrog.io/ui/native/canton-network-docker>`__
is needed to successfully pull the Digital Asset artifacts from JFrog
Artifactory.

Access to the *Daml-VPN* connection or `a SV
Node <https://docs.dev.sync.global/validator_operator/validator_onboarding.html>`__
that is whitelisted on the CN is required to connect to DevNet. The GSF
publishes a `list of SV nodes <https://sync.global/sv-network/>`__ who
have the ability to sponsor a Validator node. To access DevNet, contact
your sponsoring SV agent for VPN connection information.

If you need support accessing the SV or VPN email
support@digitalasset.com.

The CN QS is a Dockerized application and requires `Docker
Desktop <https://www.docker.com/products/docker-desktop/>`__. It is
recommended to allocate 8 GB of memory and 3 GB of Swap memory to
properly run the required Docker containers. If you witness unhealthy
containers, please consider allocating additional resources, if
possible.

DevNet is less intensive because the SVs and other LocalNet containers
are hosted outside of your local machine.

Walkthrough
===========

After the QS is installed and running, confirm that you are in the
quickstart subdirectory of the CN QS.

Open an incognito browser.

Navigate to:

localhost:3000/login

   💡 Currently, localhost URLs do not work in Safari. We are working on
   a solution and apologize for the inconvenience.

Alternatively, in the terminal, from quickstart/ run:

make open-app-ui

.. image:: /demo_images/01-logincnqs.png

Make note that the AppProvider’s username is “pat” and the password is
“abc123” (all lowercase).

Login as the AppProvider.

Fill in the login credentials: username: pat, password: abc123

.. image:: /demo_images/02-appprovider-signin.png

Select “AppInstalls” in the menu.

.. image:: /demo_images/02a-app-installs-view.png

Open a terminal.

From /quickstart/ run:

`make create-app-install-request`

This command creates an App Installation Request on behalf of the
Participant.

.. image:: /demo_images/04-create-install-req.png

   If your machine is not powerful enough to host LocalNet or if the
   docker containers are not responsive then the response may show a
   failure with status code 404 or 000. Increasing Docker memory limit
   to at least 8 GB should allow the LocalNet containers to operate
   properly.

   .. image:: /demo_images/05-error-app-install.png

Return to the browser.

The install request appears in the list.

Click “Accept”.

.. image:: /demo_images/06-install-request.png

The AppInstallRequest is Accepted. The actions update to create or
cancel the license.

.. image:: /demo_images/07-req-accept.png

Click “Create License”.

The license is created and the “# Licenses” field is updated.

.. image:: /demo_images/08-create-lic.png

In the AppProvider, “Pat the provider’s,” account, navigate to the
**Licenses** menu and select “Actions.”

.. image:: /demo_images/09-licenses-view.png

An “Actions for License” modal opens with an option to renew or expire
the license. Per the Daml contract, licenses are created in an expired
state. To activate the license, it must be renewed.

.. image:: /demo_images/10-license-modal.png

To renew the license, enter a description then click the green “Issue
Renewal Payment Request” button.

.. image:: /demo_images/11-issue-renewal.png

The license renewal process is initiated and ultimately successful.

.. image:: /demo_images/12-init-renewal.png

The license is now available for a 30-day extension for a flat fee of
$100 CC.

.. image:: /demo_images/13-license-available.png

.

Pat the provider has done as much as they are able until Alice pays the
renewal fee.

   💡For the next step we recommend opening a separate browser in
   incognito mode. Each user, AppProvider, and Org1, should be logged
   into separate browsers for most consistent results. For example, if
   you logged into AppProvider using Chrome, you would use Firefox when
   logging into Org1.

Navigate to http://localhost:3000/login using a separate browser in
incognito or private mode.

.. image:: /demo_images/01-login-cnqs.png

Login as AppUser alice.

Note that AppUser’s username is “alice” and the password is “abc123”.

.. image:: /demo_images/14-app-user-signin.png

Go to the **Licenses** View and click the “Pay renewal” button.

.. image:: /demo_images/15-license-view.png

Click on the Pay Renewal button. This navigates to the Canton Coin
Wallet log in. Click “LOG IN WITH OAUTH2”.

💡 If you have any issues with log in, navigate directly to
http://wallet.localhost:2000/.

.. image:: /demo_images/16-cc-wallet-login.png

This navigates to a keycloak login.

Enter the same username and password as before.

.. image:: /demo_images/17-keycloak-login.png

Signing in directs to the Canton Coin Wallet.

.. image:: /demo_images/18-cc-wallet-view.png

The wallet must be populated with CC in order to fulfill the
transaction.

In CC Wallet, populate the wallet with $100 USD, or the equivalent of
20,000 CC.

.. image:: /demo_images/19-populate-wallet.png

The wallet was prepopulated with 564 CC so it now contains 20,564 CC.

.. image:: /demo_images/20-wallet-bal.png

Return to the License Renewal Request as Org1. Click “Pay Renewal”.

.. image:: /demo_images/03-select-appinstalls.png

The CC Wallet balance is sufficient to send payment to the Provider.

.. image:: /demo_images/21-payment-modal.png

Return to the AppProvider’s License Renewal Requests View.

The AppProvider may now Complete the Renewal.

.. image:: /demo_images/22-complete-renewal.png

Clicking “Complete Renewal” results in a Success.

.. image:: /demo_images/23-renew-success.png

Alice’s License view shows the activated license.

.. image:: /demo_images/24-activated-license.png

Congratulations. You’ve successfully created and activated a license
with a payment transfer!

Canton Console
--------------

The Canton Console connects to the running application ledger. The
console allows a developer to bypass the UI to interact with the CN in a
more direct manner. For example, in Canton Console you can connect to
the Participant to see the location of the Participant and their
synchronizer domain.

The app provider and the app user each have their own console. To
activate the app provider’s Canton Console in a terminal from the
quickstart/ directory. Run:

`make console-app-provider`

Open the participant’s Canton Console with

`make console-app-user`

After the console initiates, run the participant and participant.domains
commands, respectively.

participant

Returns their location in the ledger.

.. image:: /demo_images/25-console-participant.png

`participant.domains`

Shows the Participant’s synchronizer.

.. image:: /demo_images/26-console-sync.png

`participant.health.ping(participant)`

Runs a health ping. The ping makes a round trip through the CN
blockchain. Pinging yourself validates communication throughout the
entire network.

.. image:: /demo_images/27-console-ping.png

Daml Shell
----------

The Daml Shell connects to the running PQS database of the application
provider’s Participant. In the Shell, the assets and their details are
available in real time.

Run the shell from quickstart/ in the terminal with:

`make shell`

Run the following commands to see the data:

`active`

Shows unique identifiers and the asset count

.. image:: /demo_images/28-shell-ids.png

active quickstart-licensing:Licensing.License:License

List the license details.

.. image:: /demo_images/29-license-details.png

active quickstart-licensing:Licensing.License:LicenseRenewalRequest

Displays license renewal request details.

archives quickstart-licensing:Licensing.AppInstall:AppInstallRequest

Shows any archived license(s).

.. image:: /demo_images/30-archive-licenses.png

Connect to DevNet
-----------------

Stop the LocalNet containers to change the connection from LocalNet to
DevNet.

In the terminal, run:

`make stop && make clean-all`

To edit the connection and observability parameters run:

`make setup`

When prompted to enable LocalNet, enter “n”. This enables DevNet

Optionally, enter “Y” to enable observability. This starts additional
containers which may require more memory for Docker.

You may leave the party hint as the default value by tapping ‘return’ on
the keyboard.

.. image:: /demo_images/31-party-hint.png

💡Running make setup regenerates `.env.local` but preserves the contents
of the `.env` file settings.

The application is now connected to DevNet.

Important: Migration ID for DevNet Connections
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When connecting to DevNet, verify that the MIGRATION_ID value in .env
matches the current network migration ID for your DevNet Super Validator
(SV).

Check the current migration ID at https://sync.global/sv-network/ under
the GSF DevNet information section.

For example, if the SV Node Information shows the migration_id value as
“0” then update MIGRATION_ID to “0” in your `.env`.

.. image:: /demo_images/32-gsf-sv.png

In `.env`:

ONBOARDING_SECRET_URL=https://sv.sv-1.dev.global.canton.network.digitalasset.com/api/sv/v0/devnet/onboard/validator/prepare

MIGRATION_ID=0

APP_PROVIDER_VALIDATOR_PARTICIPANT_ADDRESS=participant-app-provider

APP_USER_VALIDATOR_PARTICIPANT_ADDRESS=participant-app-user

Configuring Non-Default DevNet Sponsors
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In DevNet mode, you can configure a non-default SPONSOR_SV_ADDRESS,
SCAN_ADDRESS and ONBOARDING_SECRET_URL or ONBOARDING_SECRET in the
quickstart/.env file.

   💡 Connecting to DevNet requires a connection to an `approved
   SV <https://sync.global/docs/>`__. If your organization provides
   access to the DAML-VPN, then connect to it to access the Digital
   Asset-sponsored SV.

   Your organization may sponsor another `CN-approved
   SV <https://sync.global/sv-network/>`__. If this is the case, speak
   with your administrator for privileged access.

   Review the DevNet Global Synchronizer (GS) documentation to learn
   more about the `SV onboarding
   process <https://docs.dev.sync.global/validator_operator/validator_onboarding.html#onboarding-process-overview>`__.

   ⏱️ If you run into errors when making DevNet operations, double check
   that the DevNet VPN is active. DevNet VPNs may timeout, especially if
   left unattended for extended periods of time.

In an incognito browser navigate to localhost:3000/login. Login as the
Org1 user and create and archive assets, as before. Logout and do the
same as the AppProvider.

SV UIs
------

Navigate to http://sv.localhost:4000/ for the SV Web UI. The SV view
displays data directly from the validator in a GUI that is
straightforward to navigate.

Login as ‘administrator’.

.. image:: /demo_images/33-sv-ui-login.png

The UI shows information about the SV and lists the active SVs.

.. image:: /demo_images/34-active-svs.png

The Validator Onboarding menu allows for the creation of validator
onboarding secrets.

.. image:: /demo_images/35-validator-onboarding.png

Canton Coin Scan
~~~~~~~~~~~~~~~~

While connected to DevNet, navigate to the CC Scan Web UI at
http://scan.localhost:4000/.

The default activity view shows the total CC balance and the Validator
rewards.

.. image:: /demo_images/36-cc-balance.png

Select the Network Info menu to view SV identification.

.. image:: /demo_images/34-active-svs.png

The Validators menu shows that the local validator has been registered
with the SV.

.. image:: /demo_images/37-registered-validator.png

Observability Dashboard
-----------------------

In a web browser, navigate to http://localhost:3030/dashboards to view
the observability dashboards. Select “Quickstart - consolidated logs”.

.. image:: /demo_images/38-obs-dash.png

The default view shows a running stream of all services.

.. image:: /demo_images/39-service-stream.png

Change the services filter from “All” to “participant” to view
participant logs. Select any log entry to view its details.

.. image:: /demo_images/40-log-entry-details.png

Development Journey in the CN QS Lifecycle 
===========================================

The CN QS provides a foundation for developing applications on the GS.

CN QS Components
----------------

The CN QS consists of three components that the developer may find of
interest. These include development tools, LocalNet that simulates a
Global Synchronizer on your laptop, and the sample application. Each
component holds significance based on where the developer is in the
lifecycle of the application.

Development Tools
~~~~~~~~~~~~~~~~~

The development tools in CN QS provide critical infrastructure that
outlasts the sample application code. Understanding these tools informs
decisions about which components to keep, modify, or replace as your
application evolves.

**Build System**

The build system integrates Daml smart contract with the Java and
TypeScript applications. Running ./gradlew build generates code from the
Daml model, packages contracts into DAR files, and prepares deployment.

To understand the project structure, dependencies, and root project
configuration, examine quickstart/build.gradle.kts. For Daml-specific
build configurations, review quickstart/daml/build.gradle.kts.

To extend the build system for your application, create parallel project
structures in quickstart/settings.gradle.kts. These settings allow you
to maintain your code alongside the original CN QS components while
leveraging the same build infrastructure.

Customize code generation by modifying the Gradle tasks in
quickstart/buildSrc/src/main/kotlin/ to target specific languages or
adjust output formats.

As your application evolves, you can fine-tune dependency management
across language boundaries, configure artifact publishing for CI/CD
pipelines, and integrate with the Canton ledger APIs. The build system
serves as the foundation that connects your Daml models to client
applications.

When troubleshooting build issues, check the generated code in
build/generated-daml-bindings/ to verify that your Daml models are
correctly translated to your target languages.

Understanding the build system can save extensive time in development
efforts compared to creating custom build processes from scratch.

**Makefile Command Interface**

The Makefile provides standardized commands for common operations:

+-------------------------+--------------------------------------------+
| make setup              | Configure environment variables and        |
|                         | dependencies                               |
+=========================+============================================+
| make build              | Build all components (Daml, backend,       |
|                         | frontend)                                  |
+-------------------------+--------------------------------------------+
| make start              | Start the application stack                |
+-------------------------+--------------------------------------------+
| make                    | Access Canton console for the provider     |
| console-app-provider    |                                            |
+-------------------------+--------------------------------------------+
| make console-app-user   | Access Canton console for the user         |
+-------------------------+--------------------------------------------+
| make shell              | Start Daml Shell for interactive testing   |
+-------------------------+--------------------------------------------+

The Makefile serves as the primary control panel for interacting with
the CN QS environment.

Run make setup to configure environment variables in .env files.

make start applies the appropriate environment settings and orchestrates
all services through Docker Compose.

When you need direct access to the Canton ledger, use make
console-app-provider to open an interactive console session.

Makefile integrates with Gradle to trigger builds and code generation
with a single command, rather than needing to map complex Gradle tasks
directly. Examine makefile to understand all available commands to
streamline common development workflows and extend with your own custom
commands as your application evolves.

**Configuration Files**

Modify the configuration files to match your application's requirements.
Start with the Canton console configuration in
quickstart/config/canton-console/app.conf to adjust ledger access
permissions and admin operations. When you need to change network
routing or add SSL certificates, edit the NGINX configurations in
quickstart/config/nginx/ directory.

Fine-tune your observability stack by modifying the configurations in
quickstart/config/o11y/ to capture application-specific metrics and
create custom dashboards for monitoring your services. These files use
standard formats
(`HOCON <https://docs.tibco.com/pub/sfire-sfds/latest/doc/html/hocon/hocon-syntax-reference.html>`__
for Canton, `YAML <https://yaml.org/spec/1.2.2/>`__ for Docker services,
Grafana JSON for dashboards), making them easy to edit with standard
tools.

Override configuration values by setting environment variables in your
.env files rather than editing the configuration files directly. This
approach makes it easier to incorporate upstream updates by keeping your
customizations separate from the base configurations. For example, set
CANTON_ADMIN_PORT=5022 in your .env file to change the Canton admin API
port without modifying the app.conf file.

When troubleshooting, examine these configuration files to understand
how services are connected and what parameters control their behavior.
As your application grows, create additional configuration files for
your custom services following the same patterns established in the CN
QS configurations.

**Utility Tools**

Leverage the CN QS utility tools during development and testing
workflows. Use the build utilities in quickstart/buildSrc/ to automate
common development tasks. The UnpackTarGzTask helps extract archive
files while preserving permissions and symbolic links. The Java
convention scripts standardize your application's build configuration
across modules.

Configure your deployment environment by selecting the appropriate
Docker Compose files in quickstart/docker/. Use compose-validator.yaml
for validator nodes and adjust resource allocations with the
resource-constraints-*.yaml files. Start the observability stack with
docker-compose -f quickstart/docker/o11y/compose.yaml up to monitor your
application's performance. The o11y directory integrates with Grafana
dashboards defined in quickstart/config/o11y/ to provide real-time
metrics visualization.

Examine these utilities early in your development process to understand
their capabilities. Extend them to match your specific requirements
rather than building similar functionality from scratch. For example,
add custom test cases to the existing test framework or create new
deployment scripts based on the provided templates.

We recommend keeping these utilities when you replace the sample
application code. They provide infrastructure that would require
significant effort to recreate. Copy them to your application's
directory structure during the separation phase to maintain their
functionality while decoupling from the original CN QS code.

LocalNet
--------

LocalNet provides a self-contained Canton Network environment for
development and testing. It includes all necessary components to
simulate a Global Synchronizer on a single laptop without external
dependencies.

Network Components
~~~~~~~~~~~~~~~~~~

The LocalNet environment consists of three core components that work
together to simulate a Canton Network. The Application Provider and User
Validator nodes run Canton participant nodes to host your contracts and
represent user participants. Each validator operates within its own
preconfigured synchronizer.

The Global Synchronizer (GS) acts as the network coordinator through its
Super Validator (SV). It runs a Canton synchronizer node that handles
transaction ordering and conflict resolution using sequencer and
mediator services. It verifies that all network participants maintain a
consistent view of the distributed ledger.

A set of essential services supports these core components. PostgreSQL
stores the ledger data, while Keycloak handles authentication and
authorization. The Wallet Service manages digital assets and payments,
and NGINX provides routing and SSL termination for secure communication
between services.

**Technical Implementation**

The LocalNet environment is defined in the Docker Compose file:

-  quickstart/compose.yaml

Key configuration files:

-  quickstart/.env: Environment variables for the entire stack

-  quickstart/docker/localnet.env: Network-specific configuration

-  quickstart/config/canton-console/app.conf: Canton node configuration

LocalNet persists data through Docker volumes. Its network topology can
be modified to meet specific business requirements. Canton console
provides direct ledger access for debugging.

Access service logs in terminal using

`make logs`

Access git logs in terminal with

`git log`

Most teams maintain LocalNet throughout development, even after
replacing the sample application. LocalNet provides a consistent testing
platform that mirrors a production CN.

ScratchNet
----------

ScratchNet is a term that refers to a LocalNet like deployment running
on a single host that is accessible to more than one developer or
automation. It is a middle ground between LocalNet and a decentralized
DevNet. It's designed for scenarios requiring longer-running instances,
more resources, CI/CD or integration testing activities, or
multi-developer collaboration.

We’ve found that our clients prefer to set up a ScratchNet to create a
more persistent LocalNet-like environment that can also be developed
upon by a team.

**Technical Implementation**

A successful ScratchNet should include the following requirements:

-  Server or VM (recommended minimum 64GB RAM, 16 CPU cores)

-  Docker and Docker Compose

-  External storage volumes for data persistence

-  Network configuration that allows team access

**Deployment Architecture**

ScratchNet also requires persistent storage directories that are
accessible across a team. Deploying ScratchNet architecture may use the
following pattern:

# Clone CN QS repository to server

git clone https://github.com/digital-asset/cn-quickstart.git

`cd cn-quickstart`

# Create persistent storage directories

`mkdir -p /mnt/scratchnet/postgres-data`

`mkdir -p /mnt/scratchnet/canton-data`

Configure external volume mounts in a custom compose override file:

```
# scratchnet.yaml

version: '3.8'

services:

postgres-splice-app-provider:

volumes:

- /mnt/scratchnet/postgres-data/app-provider:/var/lib/postgresql/data

postgres-splice-app-user:

volumes:

- /mnt/scratchnet/postgres-data/app-user:/var/lib/postgresql/data

postgres-splice-sv:

volumes:

- /mnt/scratchnet/postgres-data/sv:/var/lib/postgresql/data

participant-app-provider:

volumes:

- /mnt/scratchnet/canton-data/app-provider:/canton-data

participant-app-user:

volumes:

- /mnt/scratchnet/canton-data/app-user:/canton-data
```

Create a basic environment configuration.

```
# .env.scratchnet

# Unique network name

DOCKER_NETWORK=scratchnet

# External hostname where ScratchNet is accessible

EXTERNAL_HOSTNAME=scratchnet.example.com

Launch with persistent volumes:

# Set up environment

export ENV_FILE=.env.scratchnet

# Launch with volume persistence

COMPOSE_FILE=quickstart/compose.yaml:scratchnet.yaml make start
```

If your team is interested in setting up a ScratchNet environment, be
sure to implement a regular, and preferably automated, backup strategy
if you want to reuse or analyze generated data. Verify that access
control is properly in place. We also suggest establishing a reliable
way to monitor resource consumption, especially for extended runs. Your
team may want to take advantage of resource management tools available
through CN’s Observability tools (Learn more in the Project Structure
Guide), or you may choose to incorporate your own lightweight tools.

For example, a monitoring script in crontab can offer basic alerting.

```
#!/bin/bash

# db-monitor.sh - Run daily to monitor database growth

THRESHOLD=80

DB_PATH="/mnt/scratchnet/postgres-data"

USAGE=$(df -h $DB_PATH \| grep -v Filesystem \| awk '{ print $5 }' \|
sed 's/%//')

SIZE=$(du -sh $DB_PATH \| awk '{ print $1 }')

echo "$(date): DB size is $SIZE, volume usage at $USAGE%" >>
/var/log/scratchnet-storage.log

if [ $USAGE -gt $THRESHOLD ]; then

echo "ScratchNet PostgreSQL volume has reached ${USAGE}% capacity
(${SIZE})"

fi
```

Containers can also be configured to automatically prune older data to
reduce latency and maintain system integrity.

participant-app-provider:

environment:

CANTON_PARAMETERS:
"--canton.participants.participant.storage.write.pruning-interval=7d"

Sample Application
------------------

The CN QS includes a complete reference application that demonstrates
Canton Network application patterns. While you'll likely replace this
component entirely, understanding its architecture provides valuable
insights for your own application design.

Application Components
~~~~~~~~~~~~~~~~~~~~~~

**Daml Models** quickstart/daml/licensing/:

-  Core business logic implemented as smart contracts

-  License and AppInstall templates demonstrate multi-party workflows

-  Integration with Splice

**Backend Service** quickstart/backend/

-  Java Spring Boot application

-  Ledger API integration for contract creation and exercise

-  REST API exposing contract operations to frontend

-  Automated code generation from Daml models

**Frontend** quickstart/frontend/

-  React/TypeScript single-page application

-  Component-based architecture with state management using React hooks

-  REST API integration with backend service

**Technical Implementation**

The API Design is defined in quickstart/common/openapi.yaml. It contains
the RESTful API definitions, establishes the JSON schema for
request/response objects, provides error handling conventions, and
creates authentication patterns.

Development Lifecycle
=====================

We’ve observed five distinct phases of the CN QS development journey.
Each phase presents unique strategies for interacting with the CN QS.

Learning Phase 
---------------

(½ - 2 weeks)

Often the first interaction with the CN QS is understanding how to get
the environment running. The next goal is to explore the application and
develop knowledge around the architecture and its workflow. It’s also
important to learn how to navigate the most common observability
dashboards and move between LocalNet and DevNet.

The most direct update strategy in this phase is to regularly update
your local copy of the CN QS by making a git pull from the main branch.

# Initial setup

`git clone https://github.com/digital-asset/cn-quickstart.git`

`cd cn-quickstart`

# Regular updates during learning

`git pull origin main`

# Environment customization (only if needed)

`echo 'export PARTY_HINT="company-name"' > .envrc.private`

`direnv allow`

Experimentation Phase
---------------------

(1-2 weeks)

In this phase, you’ll reinforce your understanding of the CN QS by
experimenting with the configurations, exploring the Ledger and CN app
APIs, and modify the Daml code, Java client, and Makefile to test
integration patterns.

At this phase, you may want to establish upstream tracking to
selectively incorporate changes.

# Set up upstream tracking

git remote add upstream
https://github.com/digital-asset/cn-quickstart.git

# Create a branch for experiments

`git checkout -b experiments`

# Periodically incorporate upstream changes

`git fetch upstream`

`git merge upstream/main`

Development Phase
-----------------

(2-3 weeks)

This is where you begin building your own application alongside the CN
QS sample application. Many developers create their new app in parallel
code directories to the CN QS application to learn from the CN QS while
building their own application.

cn-quickstart/

├── quickstart/ # Original CN QS code

│ ├── daml/ # CN QS Daml code

│ ├── backend/ # CN QS backend service

│ └── frontend/ # CN QS frontend

│

└── myapp/ # Your application code

├── daml/ # Your Daml models

├── backend/ # Your backend services

└── frontend/ # Your frontend code

Developers may generate new Daml packages, new client code in languages
other than Java or TypeScript, UI elements, CI/CD integration, and unit
tests.

Gradle Settings
~~~~~~~~~~~~~~~

When you develop parallel directories, remember to update your build
configuration to include both structures.

```
// In settings.gradle.kts

include("quickstart:daml")

include("quickstart:backend")

include("quickstart:frontend")

include("myapp:daml")

include("myapp:backend")

include("myapp:frontend")

Maintain separate build files for application components.

// In myapp/backend/build.gradle.kts

dependencies {

// Reference CN QS components if needed

implementation(project(":quickstart:daml"))

// Your specific dependencies

implementation("your.dependency:library:1.0.0")

}
```

Environment Variables
~~~~~~~~~~~~~~~~~~~~~

Use `.envrc.private` for local overrides.

# Override CN QS defaults

`export PARTY_HINT="your-company"`

`export DAML_SDK_VERSION="your-version"`

# Add your application-specific variables

`export MY_APP_CONFIG="/path/to/config"`

Create separate environment files for your application.

# In myapp/.env

`MY_APP_PORT=8080`

`MY_APP_DB_URL=jdbc:postgresql://localhost:5432/myapp`

Docker Compose
~~~~~~~~~~~~~~

Create custom compose files that extend the CN QS configuration.

# In myapp/compose.yaml

version: '3.8'

# Import the CN QS services

include:

- ../quickstart/compose.yaml

# Add your services

services:

myapp-backend:

build: ./backend

depends_on:

- postgres

- participant

environment:

- DB_URL=${MY_APP_DB_URL}

Use profiles to selectively enable groups of services.

# Start with CN QS and your services

docker-compose --profile quickstart --profile myapp up

# Start only your services (once they are able to run independently)

docker-compose --profile myapp up

Separation Phase
----------------

Over the course of a few weeks, CN developers have gained enough
experience and their new application’s complexity begins to exceed that
of the CN QS. At this point, the CN QS is no longer helpful and the
developer is advised to cut ties with the sample application.

To remove dependence on the CN QS, delete the example application
directories, adjust gradle files, change the environment variable files,
and remove the upstream connection in git.

The developer’s source code repository is disconnected from the CN QS
repository. It’s advisable to write a bridge document that maps
application components to their origins in the CN QS to create a
historical development record.

# Remove the CN QS remote

`git remote remove upstream`

# Clean up unused directories (after backing up if needed)

`rm -rf quickstart/`

# Update build files to remove CN QS references

# Edit settings.gradle.kts, build.gradle.kts, etc.

Ongoing Updates
---------------

By now, your application is likely to outgrow the capabilities of the CN
QS. However, you may want the ability to update the development tooling
or LocalNet support. The CN QS continuously adds more tooling features
and updates existing tool versions.

This process includes periodically checking into CN QS, reviewing the
ChangeLog to see what is new, and then selecting components you’d like
to include in your application. You’ll find the CN QS to be a source for
improvements, rather than a direct dependency.

We recommend establishing a regular schedule (monthly or quarterly) to
review CN QS updates.

Your update strategy may include creating a temporary clone of the CN QS
to review changes, manually incorporating them into your project, and
then removing the temporary clone.

# Temporary clone to review changes

git clone https://github.com/digital-asset/cn-quickstart.git
cn-quickstart-temp

`cd cn-quickstart-temp`

`git log --since="3 months ago" --pretty=format:"%h - %an, %ar : %s"`

# After identifying useful changes, manually incorporate them into your
project

# Then remove the temporary clone

`cd ..`

`rm -rf cn-quickstart-temp`

Every development team’s journey is unique. Adapt these strategies to
fit your specific needs, team structure, and application requirements.
As a CN developer, your goal is to find an approach that supports your
development goals while also using the CN QS as a foundation to
accelerate your development lifecycle.

Upgrades On The Global Synchronizer
===================================

The SVs periodically implement upgrades to the GS to improve
functionality, resolve issues, and introduce new features. As a node
operator or application provider you should be aware of the three types
of upgrades that may occur.

Type 1: Backward-Compatible Changes
-----------------------------------

Type 1 upgrades involve backward-compatible changes to the Splice
applications and/or modifications to the behavior of the Canton
synchronization layer. These non-breaking changes occur on Mondays,
every week.

While validators can operate effectively when behind by a Splice version
or two, the SVs recommend keeping your node up to date with weekly
upgrades. It's worth noting that "skip upgrades" (jumping multiple
versions at once) are not officially tested by the SVs, so while they
generally work, they come with increased risk.

Type 2: Daml Model Changes
--------------------------

Type 2 upgrades modify the Daml models that underlie the Splice
applications. These changes introduce a fork in the application chains
and occur every few months.

The process for Type 2 upgrades begins with distribution of the new Daml
models through Type 1 upgrades, followed by an offline Canton
Improvement Proposal (CIP) that must be approved by the SV node owners.
Next, the SVs conduct an onchain vote to establish a specific date and
time when the new models take effect. At this cutoff point, only
validators running the most recent Splice version are able to
participate in transactions using the new models. Validators that
haven't adopted the latest version are unable to participate in
transactions.

Type 3: Non-Compatible Protocol Changes
---------------------------------------

Type 3 upgrades involve fundamental changes to the Canton
synchronization protocol. These major upgrades require downtime
(sometimes called Hard Migrations) and occur every three to four months.

The implementation of Type 3 upgrades requires a Canton Improvement
Proposal (CIP) approved through an offchain vote, followed by an onchain
vote by the SVs to schedule the upgrade. These migrations impact all SVs
and Validators, requiring a coordinated transition from the prior
protocol to the new one. Currently, Canton requires all nodes to migrate
together during these upgrades.

Preparing for Upgrades
----------------------

Application providers should maintain nodes on DevNet, TestNet, and
MainNet to guarantee smooth operations during upgrades. By maintaining
nodes across all three environments you substantially increase the
likelihood that MainNet upgrades proceed without disrupting your
services or customers.

Keycloak in the CN QS
=====================

Keycloak is an open-source Identity and Access Management (IAM) solution
that provides authentication, authorization, and user management for
modern applications and services. It acts as a centralized
authentication server that handles user logins, session management, and
security token issuance.

The CN QS uses Keycloak to provide secure authentication across its
distributed architecture. Keycloak maintains separation between
authentication concerns and business logic.

Realm Structure
---------------

The CN QS defines two Keycloak realms. The AppProvider realm manages
authentication for services and users on the provider side of the
application. The AppUser realm handles authentication for the consumer
side. When components like validators or participant nodes receive
requests, they validate the authentication tokens against the
appropriate realm.

Keycloak Configuration
----------------------

The default .env configuration includes predefined users in each realm:

-  **User "Pat”** (AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME=pat)

-  **UUID**: 553c6754-8879-41c9-ae80-b302f5af92c9
      (AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID)

AppUser Realm:

-  **User "Alice"** (AUTH_APP_USER_WALLET_ADMIN_USER_NAME=alice)

-  **UUID**: 92a520cb-2f09-4e55-b465-d178c6cfe5e4
      (AUTH_APP_USER_WALLET_ADMIN_USER_ID)

-  **Password**: abc123 (AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD)

Customizing Keycloak for Business Needs
---------------------------------------

You can customize the Keycloak configuration to meet your specific
business requirements.

Accessing the Admin Console
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Keycloak Admin Console is available at:

http://keycloak.localhost:8082/admin/master/console/#/master

To log in use the default credentials:

-  **Username**: \`admin\`

-  **Password**: \`admin\`

.. image:: /demo_images/41-keycloack-login.png

Customization Scenarios
-----------------------

Add a New User
~~~~~~~~~~~~~~

1. Log in to the Keycloak Admin console

..

   .. image:: /demo_images/42-keycloak-new-user.png

2. Select the appropriate realm (AppProvider or AppUser)

..

   .. image:: /demo_images/43-keycloak-realm.png

3. Navigate to the “Users” -> “Add user”

..

   .. image:: /demo_images/44-keycloak-add-user.png

   .. image:: /demo_images/45-keycloak-user-bob.png

4. Fill in the user details and click **Create**

..

   .. image:: /demo_images/46-keycloak-bob-details.png

5. Go to the **Credentials** tab to set a password

..

   .. image:: /demo_images/47-keycloak-credentials.png

   .. image:: /demo_images/48-keycloak-set-pw.png

6. Save the password

..

   .. image:: /demo_images/49-keycloak-save-pw.png

7. You can now sign in using the new user and their password.

   a. Click **AppUser**

..

   .. image:: /demo_images/50-keycloak-oauth-login.png

   .. image:: /demo_images/51-keycloak-bob-signin.png

8. Bob is now a user

..

   .. image:: /demo_images/52-user-bob.png

Modify Client Settings
~~~~~~~~~~~~~~~~~~~~~~

1. Select the appropriate realm

2. Navigate to **Clients** -> Select the client to modify

..

   .. image:: /demo_images/53-keycloak-client-settings.png

3. Update settings per your needs

..

   .. image:: /demo_images/54-keycloak-update-settings.png

4. Save changes

Add a New Client
~~~~~~~~~~~~~~~~

1. Select the appropriate realm

2. Navigate to “Clients” -> “Create”

..

   .. image:: /demo_images/55-keycloak-add-client.png

3. Configure the client's general settings. Click **Next** for additional
      configuration options

..

   .. image:: /demo_images/56-keycloak-config-client.png

4. Configure additional settings

..

   .. image:: /demo_images/57-config-adt-settings.png

   .. image:: /demo_images/58-cofig-settings-2.png

5. Save the client

Update Environment Variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After making changes to Keycloak configuration, you may need to update
the corresponding environment variables in the .env file:

1. The Keycloak user must have the same ID as the ledger user's ID. This
      is not the party id.

2. For client changes, update the corresponding client ID and secret

3. For user changes, update the corresponding user ID and credentials

4. Restart the services to apply the changes:

make stop && make start

Troubleshooting
---------------

**Login Failures**:

1. Verify Keycloak is running: make status

..

   .. image:: /demo_images/59-verify-keycloak-running.png

Find **keycloak** near **grafana** and **loki** in the list.

**Keycloak** should show as “healthy”

   .. image:: /demo_images/60-keycloak-healthy.png

2. Check keycloak credentials in .env file

```
AUTH_APP_USER_ISSUER_URL_BACKEND=http://nginx-keycloak:8082/realms/AppUser
# for backend

AUTH_APP_USER_ISSUER_URL=http://keycloak.localhost:8082/realms/AppUser #
for backend, wallet-ui

AUTH_APP_PROVIDER_ISSUER_URL=http://keycloak.localhost:8082/realms/AppProvider
# for backend oidc client conf, wallet-ui

AUTH_APP_PROVIDER_ISSUER_URL_BACKEND=http://nginx-keycloak:8082/realms/AppProvider
# for backends
```

3. Check that the Keycloak user ID matches the ledger user ID

   a. App User

      i. Compare the **ID** value in Keycloak’s User Details with the
            AUTH_APP_USER_WALLET_ADMIN_USER_ID value in .env.

AUTH_APP_USER_WALLET_ADMIN_USER_ID=92a520cb-2f09-4e55-b465-d178c6cfe5e4

   .. image:: /demo_images/61-keycloak-alice.png

b. App Provider

..

   Compare the **ID** value in Keycloak’s User Details with the
   AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID value in .env.

   AUTH_APP_PROVIDER_WALLET_ADMIN_USER_ID=553c6754-8879-41c9-ae80-b302f5af92c9

      .. image:: /demo_images/61-keycloak-participant.png

Learn more about using Keycloak through their documentation portal:

`Keycloak Official
Documentation <https://www.keycloak.org/documentation>`__

`Keycloak Server Administration
Guide <https://www.keycloak.org/docs/latest/server_admin/>`__

`Securing Applications with
Keycloak <https://www.keycloak.org/guides.html#securing-apps>`__

Next Steps
==========

You’ve completed a business operation in the CN QS and have been
introduced to the basics of the Canton Console, Daml Shell, the SV UIs,
the GS, and Keycloak.

Learn more about Daml Shell and the project structure in the Project
Structure Guide.