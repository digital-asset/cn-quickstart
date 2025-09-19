.. _quickstart-development-journey-lifecycle:

Development journey in the CN Quickstart lifecycle
==================================================

**Contents**

`Development Journey in the CN Quickstart lifecycle <#development-journey-in-the-cn-qs-lifecycle>`__

   `CN Quickstart components <#cn-qs-components>`__

   `Development tools <#development-tools>`__

   `LocalNet <#localnet>`__

   `Network components <#network-components>`__

   `ScratchNet <#scratchnet>`__

   `Sample application <#sample-application>`__

   `Application components <#application-components>`__

The CN App Quickstart provides a foundation for developing applications on the Global Synchronizer.

CN Quickstart components
------------------------

Quickstart consists of three components: 

-  Development tools
-  ``LocalNet``, a simulated Global Synchronizer on your laptop
-  The license application.

Each component holds significance in the application development lifecycle.

Development tools
~~~~~~~~~~~~~~~~~

Development tools in CN Quickstart provide critical infrastructure that outlasts the sample application code. 
Understanding these tools helps you determine which components to keep, modify, or replace as your application evolves.

**Build system**

The build system integrates Daml smart contract with the Java and TypeScript applications. 
Running ``./gradlew`` build generates code from the Daml model, packages contracts into DAR files, and prepares deployment.

To understand the project structure, dependencies, and root project configuration, examine ``quickstart/build.gradle.kts``.
For Daml-specific build configurations, review ``quickstart/daml/build.gradle.kts``.

To extend the build system for your application, create parallel project structures in ``quickstart/settings.gradle.kts``.
These settings allow you to maintain your code alongside the original CN Quickstart components while leveraging the same build infrastructure.

Customize code generation by modifying the Gradle tasks in ``quickstart/buildSrc/src/main/kotlin/`` to target specific languages or adjust output formats.

As your application evolves, you can fine-tune dependency management across language boundaries,
configure artifact publishing for CI/CD pipelines, and integrate with the Canton ledger APIs.
The build system serves as the foundation that connects your Daml models to client applications.

When troubleshooting build issues, check the generated code in ``build/generated-daml-bindings/``
to verify that your Daml models are correctly translated to your target languages.

Understanding the build system can save extensive time in development efforts compared to creating custom build processes from scratch.

**Makefile command interface**

The ``Makefile`` provides standardized commands for common operations.

+-------------------------+--------------------------------------------+
| **Target**              | **Description**                            |
+-------------------------+--------------------------------------------+
| make setup              | Configure environment variables and        |
|                         | dependencies                               |
+=========================+============================================+
| make build              | Build all components (Daml, backend,       |
|                         | frontend)                                  |
+-------------------------+--------------------------------------------+
| make start              | Start the application stack                |
+-------------------------+--------------------------------------------+

The full list of make targets is maintained in the :ref:`make-target-reference`.
In terminal, see available targets by running ``make help`` from the ``quickstart`` directory.

The Makefile serves as the primary control panel for interacting with the CN Quickstart environment.

Run ``make setup`` to configure environment variables in ``.env`` files.

``make start`` applies the appropriate environment settings and orchestrates all services through Docker Compose.

``Makefile`` integrates with Gradle to trigger builds and code generation with a single command to avoid mapping complex Gradle tasks, directly.
Examine ``Makefile`` to understand available commands to streamline development workflows.
You can extend ``Makefile`` with your own custom commands as your application evolves.

**Configuration files**

Modify the configuration files to match your application's requirements.
Start with the Canton console configuration in ``quickstart/config/canton-console/app.conf`` to adjust ledger access permissions and admin operations.
When you need to change network routing or add SSL certificates, edit the NGINX configurations in ``quickstart/config/nginx/`` directory.

Fine-tune your observability stack by modifying the configurations in ``quickstart/docker/modules/observability/conf/``
to capture application-specific metrics and create custom dashboards for monitoring your services.
These files use standard formats (`HOCON <https://docs.tibco.com/pub/sfire-sfds/latest/doc/html/hocon/hocon-syntax-reference.html>`__
for Canton, `YAML <https://yaml.org/spec/1.2.2/>`__ for Docker services, Grafana JSON for dashboards), making them easy to edit with standard tools.

Override configuration values by setting environment variables in your ``.env`` files rather than editing the configuration files directly.
This approach makes it easier to incorporate upstream updates by keeping your customizations separate from the base configurations.
For example, set ``CANTON_ADMIN_PORT=3902`` in your ``.env`` file to change the Canton admin API port without modifying the ``app.conf`` file.

When troubleshooting, examine these configuration files to understand how services are connected and what parameters control their behavior.
As your application grows, create additional configuration files for your custom services following the same patterns established in the CN Quickstart configurations.

**Utility tools**

Leverage the CN Quickstart utility tools during development and testing workflows.
Use the build utilities in ``quickstart/buildSrc/`` to automate common development tasks.
The ``UnpackTarGzTask`` helps extract archive files while preserving permissions and symbolic links.
The Java convention scripts standardize your application's build configuration across modules.

Configure your deployment environment by selecting the appropriate Docker Compose files in ``quickstart/docker/modules/``.
Quickstart's modular architecture includes ``LocanNet``, Keycloak, observability, and other services, each with their own ``compose.yaml`` files.
Adjust resource allocations with the ``resource-constraints.yaml`` files with each module directory. 
Start the observability stack with ``docker-compose -f quickstart/docker/modules/observability/compose.yaml`` up to monitor your application's performance.
The observability module integrates with Grafana dashboards defined in ``quickstart/docker/modules/observability/conf/grafana/dashboards/`` to provide real-time metrics visualization.

You can extend these utilities to match your specific requirements to avoid building similar functionality from scratch.
For example, you might add custom test cases to the existing test framework or create new deployment scripts based on the provided templates.

We recommend keeping these utilities when you replace the sample application code.
They provide infrastructure that would require significant effort to recreate.
During the separation phase copy utilities to your application's directory structure to maintain their functionality while decoupling from the original CN Quickstart code.

LocalNet
--------

``LocalNet`` provides a self-contained Canton Network environment for development and testing.
It includes all necessary components to simulate a Global Synchronizer on a single laptop without external dependencies.

Network components
~~~~~~~~~~~~~~~~~~

The ``LocalNet`` environment consists of three core components that work together to simulate a Canton Network.
The Application Provider and User Validator nodes run Canton participant nodes to host your contracts and represent user participants.
Each validator operates within its own preconfigured synchronizer.

The Global Synchronizer acts as the network coordinator through its Super Validator (SV).
It runs a Canton synchronizer node that handles transaction ordering and conflict resolution using sequencer and mediator services.
It verifies that all network participants maintain a consistent view of the distributed ledger.

A set of essential services supports these core components.
PostgreSQL stores the ledger data, while Keycloak handles authentication and authorization.
The Wallet Service manages digital assets and payments, and NGINX provides routing and SSL termination for secure communication between services.

**Technical implementation**

The ``LocalNet`` environment is defined in the Docker Compose files:

-  quickstart/compose.yaml

-  quickstart/docker/modules/localnet/compose.yaml

Key configuration files:

-  quickstart/.env: Environment variables for the entire stack

-  quickstart/.env.local: Local environment overrides

-  quickstart/docker/modules/localnet/compose.env: LocalNet module configuration

-  quickstart/docker/modules/localnet/env/common.env: Network-specific configuration

-  quickstart/docker/modules/localnet/conf/console/app.conf: Canton console configuration

-  quickstart/docker/modules/localnet/conf/canton/: Canton node configurations

-  quickstart/docker/modules/localnet/conf/splice/: Splice application configurations

LocalNet persists data through Docker volumes. 
Its network topology can be modified to meet specific business requirements.
Canton console provides direct ledger access for debugging.

Access service logs in terminal using:

``make logs``

Access git logs in terminal with:

``git log``

Most teams maintain LocalNet throughout development, even after replacing the sample application.
``LocalNet`` provides a consistent testing platform that mirrors a production CN.

ScratchNet
----------

ScratchNet is a term that refers to a LocalNet like deployment running on a single host that is accessible to more than one developer or automation.
It is a middle ground between ``LocalNet`` and a decentralized DevNet. 
It's designed for scenarios requiring longer-running instances, more resources, CI/CD or integration testing activities, or multi-developer collaboration.

We’ve found that our clients prefer to set up a ScratchNet to create a more persistent LocalNet-like environment that can also be developed upon by a team.

**Technical implementation**

A successful ScratchNet should include the following requirements:

-  Server or VM (recommended minimum 64GB RAM, 16 CPU cores)

-  Docker and Docker Compose

-  External storage volumes for data persistence

-  Network configuration that allows team access

**Deployment architecture**

ScratchNet also requires persistent storage directories that are accessible across a team.
Deploying ScratchNet architecture may use the following pattern:

::

   # Clone CN Quickstart repository to server

   git clone https://github.com/digital-asset/cn-quickstart.git

   cd cn-quickstart

   # Create persistent storage directories

   mkdir -p /mnt/scratchnet/postgres-data

   mkdir -p /mnt/scratchnet/canton-data

Configure external volume mounts in a custom compose override file:

::

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

Create a basic environment configuration.

::

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

If your team is interested in setting up a ScratchNet environment,
be sure to implement a regular, and preferably automated,
backup strategy if you want to reuse or analyze generated data.
Verify that access control is properly in place.
We also suggest establishing a reliable way to monitor resource consumption, especially for extended runs.
Your team may want to take advantage of resource management tools available through CN’s Observability tools.

For example, a monitoring script in crontab can offer basic alerting.

::

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

Containers can also be configured to automatically prune older data to reduce latency and maintain system integrity.

participant-app-provider:

environment:

CANTON_PARAMETERS:
"--canton.participants.participant.storage.write.pruning-interval=7d"

Sample application
------------------

The CN Quickstart includes a complete reference application that demonstrates Canton Network application patterns.
While you'll likely replace this component entirely, understanding its architecture provides valuable insights for your own application design.

Application components
~~~~~~~~~~~~~~~~~~~~~~

**Daml models** quickstart/daml/licensing/:

-  Core business logic implemented as smart contracts

-  License and AppInstall templates demonstrate multi-party workflows

-  Integration with Splice

**Backend service** quickstart/backend/

-  Java Spring Boot application

-  Ledger API integration for contract creation and exercise

-  REST API exposing contract operations to frontend

-  Automated code generation from Daml models

**Frontend** quickstart/frontend/

-  React/TypeScript single-page application

-  Component-based architecture with state management using React hooks

-  REST API integration with backend service

**Technical implementation**

The API Design is defined in quickstart/common/openapi.yaml.
It contains the RESTful API definitions, establishes the JSON schema for request/response objects,
provides error handling conventions, and creates authentication patterns.
