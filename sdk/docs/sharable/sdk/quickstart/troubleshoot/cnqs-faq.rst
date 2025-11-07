.. _quickstart-faq:

=================================
Canton Network App Quickstart FAQ
=================================

**Contents**

`CN App Quickstart Frequently Asked Questions <#cn-qs-frequently-asked-questions>`__

   `System requirements & setup <#system-requirements-setup>`__

   `Common issues & troubleshooting <#common-issues-troubleshooting>`__

   `Development & testing <#development-testing>`__

   `Infrastructure & environment <#infrastructure-environment>`__

   `Best practices & common
   pitfalls <#best-practices-common-pitfalls>`__

   `Database & query access <#database-query-access>`__

`CN App Quickstart Make target reference <#cn-qs-make-target-reference>`__

   `UI opening commands <#ui-opening-commands>`__

`LocalNet URLs <#localnet-urls>`__

**CN App Quickstart Frequently Asked Questions**
================================================

**System requirements & setup**
-------------------------------

**Have the best technologies been selected for the CN App Quickstart?**

The Quickstart is designed to help teams become familiar with Canton Network (CN) application development by providing scaffolding to kickstart development.
Quickstart is intended to be incrementally extended by you to meet your specific business needs.
Once you are familiar with the Quickstart, please review the technology choices and the application design to determine what changes are needed - technology and design decisions are ultimately up to you.
Please be aware that the Canton Network Application Quickstart (CN App Quickstart) is a rapidly evolving work in progress.

**What are the minimum system requirements to run CN App Quickstart LocalNet?**

The CN App Quickstart requires Docker Desktop with at least 8 GB of memory allocated to run ``LocalNet`` properly.
If your machine has less memory, consider declining Observability when prompted during setup.

**Which browsers are supported for running CN App Quickstart?**

Chromium browsers such as Chrome, Edge, and Firefox are recommended.
Safari has known issues with local URLs and should be avoided.
You may also use the same browser with one user in incognito mode and the other in standard mode.

**How should I test Participant and User interactions on LocalNet?**

For testing multiple users, use separate browsers or one browser in standard mode and another in incognito to avoid session/cookie interference.

**Where can I install CN App Quickstart dependencies?**

This repository uses ``direnv``, ``nix``, and ``docker-compose`` to provide development dependencies:

* how to `install direnv <https://direnv.net/docs/installation.html>`__
* how to `install nix <https://nix.dev/install-nix.html>`__
* how to `install docker-compose <https://docs.docker.com/compose/install/>`__

**Do I need to install Nix?**

You can use the project files in the ``quickstart`` directory as a standalone project without nix, but you will need to provide binary dependencies manually.
We do not offer guidance on how to do this, but you can find the required binaries in the ``quickstart/compose.yaml`` file.

**Why is Nix-shell unable to download my SSL certificate?**

The Nix prerequisite may introduce hurdles to installation if your enterprise runs behind a corporate proxy.
If nix-shell is not found, then verify that ``/nix/var/nix/profiles/default/etc/ssl/certs/ca-bundle.crt``
contains your corporate CA.

CN, PQS, Daml Shell and other CN App Quickstart related services run on a user-supplied JVM.
CN App Quickstart assumes that you have access to JVM v17+ with access to the internet.
If your organization operates behind a web proxy then JVM may not have automatic knowledge of the corporate certificate.
In these instances, JVM must be instructed to trust the certificate.

If Nix-related errors occur, verify that the correct certificates exist by looking at the log file.

::

   $ sudo HOME=/var/root
   NIX_SSL_CERT_FILE=/nix/var/nix/profiles/default/etc/ssl/certs/ca-bundle.crt
   /nix/store/dfqs9x0l0r4dn7zjp1hymmv9wvpp9x2k-nix-2.26.2/bin/nix-channel
   --update nixpkgs

If the log returns an error message such as:

::

   error: unable to download 'https://nixos.org/channels/nixpkgs-unstable':
   SSL peer certificate or SSH remote key was not OK (60)

Then the required corporate CA does not exist.
Request your corporate CA from your organization’s tech administrator and merge the certificate into the Nix ``certs ca-bundle.crt``.

If you need additional support, the `Nix reference manual <https://nix.dev/manual/nix/2.24/command-ref/conf-file.html#conf-ssl-cert-file>`__
offers guidance regarding the order at which cert files are detected and used on the host, as well as environment variables to override default file locations.

Graham Christensen’s Determinate Systems blog offers a solution for Nix
`corporate TLS certificates <https://determinate.systems/posts/zscaler-macos-and-nix-on-corporate-networks/>`__
problems on MacOS.
The NixOS team forked this solution as an `experimental installer <https://github.com/NixOS/experimental-nix-installer>`__
that is stable on most operating systems.

**Should I build with make or gradle?**

The gradle daemon has been disabled to prevent parallel processing of transcodegen.

Gradle tasks had been known to create order and concurrency issues which caused files to get cleaned too early.
Always prefer to use the make commands.

**What Daml SDK version should I use?**

Please refer to the `MainNet <https://docs.dev.sync.global/app_dev/overview/version_information.html>`__ 
and `TestNet <https://docs.test.sync.global/app_dev/overview/version_information.html>`__ 
Splice documentation for guidance regarding version information.

**How do I set the Daml SDK version?**

The Daml SDK version is set in ``.env`` as ``DAML_RUNTIME_VERSION``.

You can run ``make install-daml-sdk SDK_VERSION`` at any time to download and install another version of the daml sdk.

**How do I obtain Splice contract DAR files?**

All Splice DARs are available in the `Splice repository <https://github.com/hyperledger-labs/splice/tree/main/daml/dars>`__.

**What version of the Java SDK does the CN Quickstart use?**

The CN App Quickstart uses Java SDK version ``Eclipse Temurin JDK version 21``.
The Java SDK runs within the Docker container.

This information is specified in ``quickstart/compose.yaml`` and ``.env``, respectively.

``quickstart/compose.yaml``

::

   services:
   backend-service:
      image: "eclipse-temurin:${JAVA_VERSION}"

``.env``

::

   JAVA_VERSION=21-jdk

**How do I resolve a “build failed with an exception failure”?**

If ``make install-daml-sdk`` results in:

::

   Task :daml:unpackDamlSdk FAILED
   FAILURE: Build failed with an exception

Then you may have a corrupted ``daml-sdk snapshot``.
In most cases, deleting the identified tarball snapshot will resolve the issue in subsequent installation attempts.

This error may occur if ``make install-daml-sdk`` is interrupted.

A failure of this kind will end in:

::

   https://github.com/digital-asset/daml/releases/download/v3.3.0-snapshot.20241031.13398.0.vf95d2607/daml-sdk-3.2.0-snapshot.20241031.13398.0.vf95d2607-macos-x86_64-ee.tar.gz to /Users/USER/Code/cn-quickstart/quickstart/daml/.sdk/daml-sdk-3.2.0-snapshot.20241031.13398.0.vf95d2607-macos-x86_64-ee.tar.gz

   > Task :daml:unpackDamlSdk FAILED

   FAILURE: Build failed with an exception.

   \* What went wrong:

   Execution failed for task ':daml:unpackDamlSdk'.

   > java.io.EOFException

To resolve this error, copy the faulty ``.tar.gz`` file with directory path as shown in *your* terminal and ``rm`` it:

::

   rm /Users/USER/Code/cn-quickstart/quickstart/daml/.sdk/daml-sdk-3.2.0-snapshot.20241031.13398.0.vf95d2607-macos-x86_64-ee.tar.gz

.. note:: ``USER`` in ``/Users/USER/`` will display your username. Copy and paste from your terminal. NOT this FAQ.

Reattempt make ``install-daml-sdk``.

**How do I resolve Docker containers that fail unexpectedly?**

Starting the CN App Quickstart while running Docker Desktop version 4.38.0 may result in java.lang.NullPointerException errors:

::

   2025-01-31 15:12:55,705 ERROR [41 lity] FixedSizeBlockingPool [] Pool object could not be added due to exception:
   java.lang.NullPointerException: Cannot invoke
   "jdk.internal.platform.CgroupInfo.getMountPoint()" because "anyController" is null [ ] Exception in thread "Native-Process-Pool-1-17"

This error indicates that ORDS containers (an Oracle web client in Java) cannot properly load during startup.

First, verify that you are not running Docker Desktop version 4.38.0 (181591).

To view the version number, open Docker Desktop. In the top menu, select **Docker Desktop -> About**.

.. image:: images/docker-desktop-v4390-about.png
   :alt: Docker Desktop version 4.39.0

If you are running Version 4.38.0 then upgrade Docker Desktop and make a new attempt.

If you’d like to learn more about this issue, visit `JDK Bug System <https://bugs.openjdk.org/browse/JDK-8348566>`__.

**Common issues & troubleshooting**
-----------------------------------

**How can I check if my CN App Quickstart deployment is running correctly?**

Use ``make status`` to see all running containers and their health status.

**What should I do if containers show as "unhealthy" after startup?**

The most common cause is insufficient memory allocation to Docker. Try:

1. Increase Docker memory allocation to at least 8 GB

2. Run ``make stop; make clean-all; make clean-docker``

3. Run ``make setup`` and turn off ``observability``

4. Restart with ``make start``

**How can I monitor system metrics?**

You can use Grafana at http://localhost:3030/ to monitor system metrics if ``observability`` is enabled.

For more information see the Observability and Troubleshooting Overview.

**What should I do if I need to completely reset my environment?**

Execute the following commands in order:

1. ``make stop``

2. ``make clean-all``

3. ``make setup`` (to reconfigure environment options)

4. ``make start``

**How do I resolve "Couldn't find env file" in make build?**

If you receive an error message such as:

::

   Couldn't find env file: /Users/USER/development/canton/cn-quickstart/quickstart/.env.local
   make: \**\* [build-docker-images] Error 15

Run ``make setup`` to create the ``.env.local`` file.

**Development & testing**
-------------------------

**How do I access the Daml Shell for debugging?**

Run ``make shell`` from the quickstart directory.
This provides access to useful commands like:

-  ``active`` - shows summary of contracts

-  ``active quickstart:Main:Asset`` - shows Asset contract details

-  ``contract [contract-id]`` - shows full contract details

**How can I monitor application logs and traces?**

The CN App Quickstart provides several observability options:

1. Direct container logs: ``docker logs <container-name>``

2. Grafana dashboards: http://localhost:3030/

3. Consolidated logs view in Grafana

**Why do backend configurations disappear after restarting the backend service?**

Backend service configurations (including tenant registrations, application state, and service registrations) are stored in memory and lost when the service is restarts.
Run ``make restart-backend`` to rebuild and restart the backend service with configurations restored.

Persistent storage is in our roadmap, but has not been implemented yet.

**How can I resolve a backend build failure?**

Build failures may occur if the Daml SDK is not installed or is corrupted.
If you see errors on ``make build`` such as:

::

   > Task :backend:extractProto FAILED

   FAILURE: Build failed with an exception.

   * What went wrong:
   Execution failed for task ':backend:extractProto'.
   > Could not resolve all files for configuration ':backend:protobuf'.
      > Could not find com.daml:ledger-api-proto:.
      Required by:
            project :backend

Then verify that the ``.env`` file contains an accurate daml SDK value in ``DAML_RUNTIME_VERSION=``.

Find the daml SDK version with ``make check-daml-sdk``.

**Infrastructure & environment**
--------------------------------

**What's the difference between LocalNet and DevNet deployment?**

``LocalNet`` runs everything locally including a Super Validator and Canton Coin wallet, making it more resource intensive but self-contained.

``DevNet`` is NOT included in the CN App Quickstart.
However, CN ``DevNet`` connects to actual decentralized Global Synchronizer infrastructure operated by Super Validators and requires whitelisted VPN access and connectivity.

For more information see the Project Structure Overview.

**What is ScratchNet?**

``ScratchNet`` is a persistent Canton Network environment that supports team collaboration while maintaining centralized control.
It fills the gap between a single-developer LocalNet (constrained by one laptop's resources) and a fully decentralized DevNet (maintained by Super Validators).
Development teams typically deploy ``ScratchNet`` on a shared server to enable longer-running instances with persistent data storage across development sessions.

**How can I find out the migration_id of DevNet?**

Go to https://sync.global/sv-network/ and look for the ``migration_id`` value.

**Do I need VPN access to use CN App Quickstart?**

No. VPN access is only required for ``DevNet`` connections.
The CN App Quickstart only provides a ``LocalNet`` deployment option, which does not require VPN access.

**What will I need when I am ready to connect to DevNet?**

To connect to CN ``DevNet`` you need access to a SV Node that is whitelisted on the CN. 
Contact your sponsoring Super Validator agent for connection information.

**How can I operate a ``DevNet`` node?**

Use the Docker compose ``DevNet`` `deployment from Splice <https://docs.dev.sync.global/validator_operator/validator_compose.html>`__.
We recommend using the Docker compose configuration for integration and long-term testing.

**Where do I start when I am ready to run an application on my node?**

Refer to the :externalref:`CN Utilities documentation <utility-prerequisites>` for application-specific deployment instructions with an example.
As a general rule, deploying an app on your node requires installing the application DARs. 
CN Utilities also includes an image that supports uploading the DARs within your cluster.

**How does my team gain access to an enterprise license?**

Get an Enterprise evaluation license by filling out our `contact form <https://www.digitalasset.com/contact-us?comments=I'm%20requesting%20access%20to%20jFrog>`__.
The evaluation license is good for six months and is renewable.
Please allow 24 hours for the request to be processed.
The evaluation license includes VPN access to ``DevNet``.

**How do I log in with Keycloak?**

The CN App Quickstart uses Keycloak for authentication when ``OAUTH2`` is enabled.
If you have issues with logging in with Keycloak credentials, you may begin troubleshooting by running make status to verify the Keycloak service is running.

Keycloak should show healthy.

::

   keycloak   quay.io/keycloak/keycloak:26.1.0 "/opt/keycloak/bin/k…"
   keycloak   17 minutes ago Up 17 minutes (healthy) 8080/tcp, 8443/tcp, 9000/tcp

Keycloak credentials are set in ``oauth2.env`` with the following credentials:

::

   Username: AUTH_APP_USER_WALLET_ADMIN_USER_NAME (e.g. app-user)
   Password: AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD (e.g. abc123)

The Keycloak user must have the same ID as the ledger user’s ID.
This should be reflected in the default behavior.

**How can I begin using lnav with the Quickstart application on MacOS?**

On MacOS, ``lnav`` can be installed with ``brew install lnav``.
Running ``make capture-logs`` requires ``coreutils`` to be installed with ``brew install coreutils``.

**Best practices & common pitfalls**
------------------------------------

**How should I handle multiple user testing in the local environment?**

Best practices include:

1. Use separate browsers for different users

2. Follow proper logout procedures between user switches

3. Be aware that even incognito mode in the same browser may have session interference

4. Consider using the make commands for testing specific operations (e.g., ``make create-app-install-request``)

**Database & query access**
---------------------------

**What's the recommended way to query ledger data?**

The Participant Query Store (PQS) is recommended for querying ledger data.

.. _make-target-reference:

**CN App Quickstart Make target reference**
===========================================

+---------------------+------------------------------------------------+
| **Target**          | **Description**                                |
+=====================+================================================+
| build               | Build frontend, backend, Daml model and docker |
|                     | images.                                        |
+---------------------+------------------------------------------------+
| build-backend       | Build the backend service.                     |
+---------------------+------------------------------------------------+
| build-daml          | Build the Daml model.                          |
+---------------------+------------------------------------------------+
| build-frontend      | Build the frontend application.                |
+---------------------+------------------------------------------------+
| canton-console      | Start the Canton console. Connects to the      |
|                     | running app provider, app-user, sv ledgers.    |
+---------------------+------------------------------------------------+
| capture-logs        | Consumes Docker events and starts capturing    |
|                     | logs to ``/logs`` directory for each service   |
|                     | when a ``start`` Docker event is observed.     |
|                     | Ideal for diagnostic purposes.                 |
+---------------------+------------------------------------------------+
| clean               | Clean the build artifacts.                     |
+---------------------+------------------------------------------------+
| clean-all           | Stop and remove all build artifacts, Docker    |
|                     | containers and volumes.                        |
+---------------------+------------------------------------------------+
| clean-application   | Like ``clean-docker``, but leaves              |
|                     | observability services running.                |
+---------------------+------------------------------------------------+
| clean-canton-       | Stop and remove the Canton console container.  |
| console             |                                                |
+---------------------+------------------------------------------------+
| clean-docker        | Stop and remove application Docker containers  |
|                     | and volumes.                                   |
+---------------------+------------------------------------------------+
| clean-shell         | Stop and remove the Daml Shell container.      |
+---------------------+------------------------------------------------+
| compose-config      | Displays finalized configuration for each      |
|                     | service initiated by ``make start``.           |
|                     | Dynamic environment variables                  |
|                     | e.g. ``APP_PROVIDER_PARTY`` are resolved at    |
|                     | run time and not included in this output.      |
+---------------------+------------------------------------------------+
| create-             | Submit an App Install Request from the App     |
| app-install-request | User participant node.                         |
+---------------------+------------------------------------------------+
| generate-NOTICES    | Generate the NOTICES.txt file.                 |
+---------------------+------------------------------------------------+
| install-daml-sdk    | Install the Daml SDK.                          |
+---------------------+------------------------------------------------+
| logs                | Show logs of Docker containers.                |
+---------------------+------------------------------------------------+
| restart             | Restart the application services.              |
+---------------------+------------------------------------------------+
| restart-backend     | Build and restart the backend service.         |
+---------------------+------------------------------------------------+
| restart-frontend    | Build and restart the frontend application.    |
+---------------------+------------------------------------------------+
| setup               | Configure local development environment.       |
+---------------------+------------------------------------------------+
| shell               | Start Daml Shell. Connects to the running      |
|                     | application PQS database.                      |
+---------------------+------------------------------------------------+
| start               | Start the application and observability        |
|                     | services if enabled.                           |
+---------------------+------------------------------------------------+
| status              | Show status of Docker containers.              |
+---------------------+------------------------------------------------+
| stop                | Stop the application and observability         |
|                     | services.                                      |
+---------------------+------------------------------------------------+
| stop-application    | Stop only the application, leaving             |
|                     | observability services running.                |
+---------------------+------------------------------------------------+
| tail                | Tail logs of Docker containers.                |
+---------------------+------------------------------------------------+
| update-env-         | Helper to update DAML_RUNTIME_VERSION in .env  |
| sdk-runtime-version | based on daml/daml.yaml sdk-version.           |
+---------------------+------------------------------------------------+

**UI Opening Commands**
-----------------------

+------------------+---------------------------------------------------+
| **Target**       | **Description**                                   |
+==================+===================================================+
| open-app-ui      | Open the Application UI in the active browser     |
+------------------+---------------------------------------------------+
| open-observe     | Open the Grafana UI in the active browser         |
+------------------+---------------------------------------------------+
| open-sv-wallet   | Open the Super Validator wallet UI in the active  |
|                  | browser                                           |
+------------------+---------------------------------------------------+
| o                | Open the Super Validator interface UI in the      |
| pen-sv-interface | active browser                                    |
+------------------+---------------------------------------------------+
| open-sv-scan     | Open the Super Validator Scan UI in the active    |
|                  | browser                                           |
+------------------+---------------------------------------------------+
| open             | Open the App User wallet UI in the active browser |
| -app-user-wallet |                                                   |
+------------------+---------------------------------------------------+

**LocalNet URLs**
=================

+-------------------------------+--------------------------------------+
| **URL**                       | **Description**                      |
+===============================+======================================+
| http://localhost:3000         | Main application UI                  |
+-------------------------------+--------------------------------------+
| http://localhost:3030         | Grafana observability dashboard (if  |
|                               | enabled)                             |
+-------------------------------+--------------------------------------+
| http://localhost:4000         | Super Validator gateway - lists      |
|                               | available web UI options             |
+-------------------------------+--------------------------------------+
| http://wallet.localhost:2000  | Canton Coin wallet interface         |
+-------------------------------+--------------------------------------+
| http://sv.localhost:4000      | Super Validator Operations           |
+-------------------------------+--------------------------------------+
| http://scan.localhost:4000    | Canton Coin Scan web UI - shows      |
|                               | balances and validator rewards       |
+-------------------------------+--------------------------------------+
| http://localhost:7575         | Ledger API service                   |
+-------------------------------+--------------------------------------+
| http://localhost:5003         | Validator API service                |
+-------------------------------+--------------------------------------+

Sponsoring Super Validators may offer different URLs for their ``DevNet`` services.
