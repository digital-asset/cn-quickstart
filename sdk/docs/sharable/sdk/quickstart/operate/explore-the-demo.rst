======================================================
Explore the Canton Network Application Quickstart demo
======================================================

**Contents**

`Exploring the demo <#exploring-the-demo>`__

`Prerequisites <#prerequisites>`__

`Walkthrough <#walkthrough>`__

   `Canton Console <#canton-console>`__

   `Daml Shell <#daml-shell>`__

   `Connect to DevNet <#connect-to-devnet>`__

   `Important: migration ID for DevNet connections <#important-migration-id-for-devnet-connections>`__

   `Configuring non-default DevNet sponsors <#configuring-non-default-devnet-sponsors>`__

   `SV UIs <#sv-uis>`__

   `Canton Coin Scan <#canton-coin-scan>`__

   `Observability dashboard <#observability-dashboard>`__

`Next Steps <#next-steps>`__

.. _exploring-the-demo:

Exploring the demo
==================

Explore the demo is intended to help you become familiar with a Canton Network (CN) business operation within the CN App Quickstart.
The App Quickstart application is intended to be incrementally extended by your team to meet your business needs.
When you are familiar with the App Quickstart, review the technology choices and application design to determine what changes are needed.
Technology and design decisions are ultimately up to you.

As a result, the CN App Quickstart guides may be a little out of step with the application.
If you find errors or other inconsistencies, please contact your representative at Digital Asset.

This section works through a complete business operation within the CN App Quickstart.

Prerequisites
=============

You should have successfully `installed the CN App Quickstart <../download/cnqs-installation.html>`__
before beginning this demonstration.

Access to the `CN Docker repository <https://digitalasset.jfrog.io/ui/native/canton-network-docker>`__
is needed to successfully pull the Digital Asset artifacts from JFrog Artifactory.

If you need support accessing the JFrog Artifactory, please email support@digitalasset.com.

Access to the *Daml-VPN* connection or `a SV Node <https://docs.dev.sync.global/validator_operator/validator_onboarding.html>`__
that is whitelisted on the CN is required to connect to ``DevNet``.
The GSF publishes a `list of SV nodes <https://sync.global/sv-network/>`__ who have the ability to sponsor a Validator node.
To access ``DevNet``, contact your sponsoring SV agent for VPN connection information.

If you need support accessing the SV or VPN email support@digitalasset.com.

The CN App Quickstart is a Dockerized application and requires `Docker Desktop <https://www.docker.com/products/docker-desktop/>`__.
It is recommended to allocate 8 GB of memory to properly run the required Docker containers.
If you witness unhealthy containers, please consider allocating additional resources, if possible.

``DevNet`` is less intensive because the SVs and other ``LocalNet`` containers are hosted outside of your local machine.

Walkthrough
===========

The CN App Quickstart can run with or without authorization, based on your business needs.
Toggle authorization with the ``make setup`` command in the ``quickstart`` subdirectory.
``make setup`` asks to enable ``LocalNet``, Observability, OAUTH2, and specify a party hint.
In this demo, we use ``LocalNet``, the default party hint, and we show OAUTH2 as enabled and disabled.
When OAUTH2 makes a difference, we display both paths one after the other. 
You can follow your path and ignore the other.
You may enable Observability, but it is not required for this demo.

Choose your adventure:

``make setup`` without OAUTH2:

.. image:: images/make-setup-no-auth.png
   :alt: Make setup no auth
   :width: 70%

``make setup`` with OAUTH2:

.. image:: images/make-setup-with-auth.png
   :alt: Make setup with auth

Build and start App Quickstart:

.. code-block:: bash
   
   make build; make start

Open an incognito browser.

Navigate to:

..

   localhost:3000

Alternatively, in the terminal, from quickstart/ run:

``make open-app-ui``

When OAUTH2 is disabled, the homepage presents a simple login field.
Begin by logging in as the ``AppProvider`` by entering "app-provider" in the User field.

.. image:: images/01-login-app-qs-noauth.png
   :alt: CN App Quickstart Login screen without Auth

When OAUTH2 is enabled, the homepage prompts to login with Keycloak's OAuth 2.0 portal:

.. image:: images/01-login-app-qs-auth.png
   :alt: CN App Quickstart Login screen with Auth

Make a mental note that ``AppProvider``’s username is “app-provider” and the password is "abc123" (all lowercase).

Login with ``app-provider`` with keycloak.

Fill in the login credentials: username: app-provider, password: abc123

.. image:: images/login-app-provider-view.png
   :alt: AppProvider login screen

Once you are logged in select “AppInstalls” in the menu.

.. image:: images/appinstalls-default-view.png
   :alt: App Installs view

Open a terminal to create an app install request.

From ``/quickstart/`` run:

``make create-app-install-request``

This command creates an App Installation Request on behalf of the Participant.

.. image:: images/04-create-install-req.png
   :alt: App Install Request

.. note:: If your machine is not powerful enough to host ``LocalNet`` or if the docker containers are not responsive then the response may show a failure with status code 404 or 000 (as shown in the image below). Increasing Docker memory limit to at least 8 GB should allow the ``LocalNet`` containers to operate properly.

.. image:: images/05-error-app-install.png
   :alt: App Install Request error

Return to the browser.

The install request appears in the list.

Click “Accept”.

.. image:: images/app-installs-new-install-request.png
   :alt: install request

The ``AppInstallRequest`` is Accepted. The actions update to create or cancel the license.
Click “Create License”.

.. image:: images/accept-app-install-request.png
   :alt: accept request

The license is created and the “# Licenses” field is updated.

.. image:: images/create-license-success.png
   :alt: create license

Next, navigate to the "Licenses" menu and select “Actions.”

.. image:: images/licenses-view.png
   :alt: Licenses view

An “Actions for License” modal opens with an option to renew or expire the license.
Per the Daml contract, licenses are created in an expired state.
To activate the license, a renewal payment request must be issued.
Enter a description for the license renewal request, then click the green “Issue Renewal Payment Request” button.

.. image:: images/activate-license-modal.png
   :alt: issue renewal

The license renewal process is initiated and a 30-day extension becomes available for a fee of $100 CC.

.. image:: images/license-renewal-request-success.png
   :alt: license available

The app-provider has done as much as they are able until the app-user pays the renewal fee.

   💡For the next step we recommend opening a separate browser in incognito mode.
   Each user should be logged into separate browsers for most consistent results.
   For example, if you logged into ``AppProvider`` using Chrome, you would use Firefox when logging into ``AppUser``.

Navigate to http://localhost:3000/ using a separate browser in incognito or private mode.

Your login screen will look as it had when you logged in as ``AppProvider``.
If OAUTH2 is disabled, simply log in as ``app-user``.

.. image:: images/login-app-user-noauth.png
   :alt: AppUser login screen without Auth

When OAUTH2 is enabled, you log in using the app-user username and password.

.. image:: images/01-login-app-qs-auth.png
   :alt: login screen

Login as ``AppUser`` with “app-user" as the username and the password is “abc123”.

.. image:: images/appuser-auth-login-view.png
   :alt: AppUser login screen

As the app-user, go to the **Licenses** view and click the “Pay renewal” button.

.. image:: images/appuser-licenses-view.png
   :alt: License view

When OAUTH2 is disabled, you are directed to log in to the Canton Wallet, directly.
Use "app-user" as the username.

.. image:: images/appuser-canton-coin-wallet-login-noauth.png
   :alt: AppUser Canton Coin no auth

When OAUTH2 is enabled, you log in to the Canton Coin Wallet by clicking “LOG IN WITH OAUTH2”.

.. image:: images/16-cc-wallet-login.png
   :alt: CC Wallet login

This navigates to a keycloak login.

Enter the app-user username and password.

.. image:: images/app-user-reauth.png
   :alt: appuser reauth login
   :width: 60%

Signing in navigates to a preloaded Canton Coin Wallet.
Click **Send Payment**.

.. image:: images/cc-wallet-send-payment.png
   :alt: CC Wallet view

Return to the ``AppProvider``’s License Renewal Requests View.
The ``AppProvider`` may now Complete the Renewal.

.. image:: images/app-provider-complete-renewal.png
   :alt: complete renewal

Clicking “Complete Renewal” results in a Success.

.. image:: images/renew-license-success.png
   :alt: renewal success

The App User's License view shows the activated license.

.. image:: images/app-user-activated-license.png
   :alt: Activated license

Congratulations. You’ve successfully created and activated a license with a payment transfer!

Canton Console
--------------

The :externalref:`Canton Console<canton_console>` connects to the running application ledger.
The console allows a developer to bypass the UI to interact with the CN in a more direct manner.
For example, in Canton Console you can connect to the Participant to see the location of the Participant and their synchronizer domain.

Activate the :externalref:`Canton Console<canton_remote_console>` in a terminal from the ``quickstart/`` directory.
Run:

``make canton-console``

After the console initiates, run the ``participants`` and ``participants.all`` commands, respectively.

``participants``

Returns a detailed categorization of participants.

.. image:: images/canton-console-participants.png
   :alt: Participant location in the ledger

``participants.all``

Shows a list of all participant references.

.. image:: images/canton-console-participants.all.png
   :alt: Participant synchronizer

``health.status``

Is a diagnostic tool that displays the health of Canton Network participants.

.. image:: images/health.status.png
   :alt: Ping yourself

Daml Shell
----------

The :externalref:`Daml Shell <build_daml_shell_component_howto>` connects to the running PQS database of the application provider’s Participant.
In the Shell, the assets and their details are available in real time.

Run the shell from quickstart/ in the terminal with:

``make shell``

Run the following commands to see the data:

``active``

Shows unique identifiers and the asset count

.. image:: images/28-shell-ids.png
   :alt: Active identifiers

``active quickstart-licensing:Licensing.License:License``

List the license details.

.. image:: images/29-license-details.png
   :alt: License details

``active quickstart-licensing:Licensing.License:LicenseRenewalRequest``

Displays license renewal request details.

.. image:: images/active-quickstart-appinstallrequest.png
   :alt: License renewal request details

``archives quickstart-licensing:Licensing.AppInstall:AppInstallRequest``

Shows any archived license(s).

.. image:: images/30-archive-licenses.png
   :alt: Archived licenses

Connect to DevNet
-----------------

Stop the ``LocalNet`` containers to change the connection from ``LocalNet`` to ``DevNet``.

In the terminal, run:

``make stop && make clean-all``

To edit the connection and observability parameters run:

``make setup``

When prompted to enable ``LocalNet``, enter “n”. This enables ``DevNet``

Optionally, enter “Y” to enable observability. This starts additional containers which may require more memory for Docker.

.. note:: Observability may no longer work while App Quickstart is under revisions. If you experience unexpected errors, set observability to "n". 

You may leave the party hint as the default value by tapping ‘return’ on the keyboard.

.. image:: images/31-party-hint.png
   :alt: Party hint

💡Running make setup regenerates ``.env.local`` but preserves the contents of the ``.env`` file settings.

The application is now connected to ``DevNet``.

Important: Migration ID for DevNet connections
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When connecting to ``DevNet``, verify that the ``MIGRATION_ID`` value in ``.env`` matches the current network migration ID for your ``DevNet`` Super Validator (SV).

Check the current migration ID at https://sync.global/sv-network/ under the GSF ``DevNet`` information section.

For example, if the SV Node Information shows the ``migration_id`` value as “2” then update ``MIGRATION_ID`` to “2” in your ``.env``.

.. note:: Some ``env`` vars will be in different files. For example, ``/env/dev.env``

.. image:: images/gsf-devnet-sv-info.png
   :alt: GSF SV information

In ``.env``:

..

   ONBOARDING_SECRET_URL=https://sv.sv-1.dev.global.canton.network.digitalasset.com/api/sv/v0/devnet/onboard/validator/prepare

   MIGRATION_ID=2

   APP_PROVIDER_VALIDATOR_PARTICIPANT_ADDRESS=participant-app-provider

   APP_USER_VALIDATOR_PARTICIPANT_ADDRESS=participant-app-user

Configuring non-default DevNet sponsors
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. note:: Configuration files are currently under revision and may not be in the stated locations.

In ``DevNet`` mode, you can configure a non-default ``SPONSOR_SV_ADDRESS``, ``SCAN_ADDRESS`` and ``ONBOARDING_SECRET_URL`` or ``ONBOARDING_SECRET`` in the ``quickstart/.env`` file.

   💡 Connecting to ``DevNet`` requires a connection to an `approved SV <https://sync.global/docs/>`__.
   If your organization provides access to the DAML-VPN, then connect to it to access the Digital Asset-sponsored SV.

   Your organization may sponsor another `CN-approved SV <https://sync.global/sv-network/>`__.
   If this is the case, speak with your administrator for privileged access.

   Review the ``DevNet`` Global Synchronizer documentation to learn more about the `SV onboarding process <https://docs.dev.sync.global/validator_operator/validator_onboarding.html#onboarding-process-overview>`__.

   ⏱️ If you run into errors when making ``DevNet`` operations, double check that the ``DevNet`` VPN is active.
   ``DevNet`` VPNs may timeout, especially if left unattended for extended periods of time.

In an incognito browser navigate to ``localhost:3000/login``.
Login as the Org1 user and create and archive assets, as before.
Logout and do the same as the ``AppProvider``.

Canton Coin Scan
~~~~~~~~~~~~~~~~

While connected to ``DevNet``, navigate to the CC Scan Web UI at http://scan.localhost:4000/.

The default activity view shows the total CC balance and the Validator rewards.

.. image:: images/36-cc-balance.png
   :alt: CC balance
   :width: 45%

Select the Network Info menu to view SV identification.

.. image:: images/34-active-svs.png
   :alt: Active SVs

The Validators menu shows that the local validator has been registered with the SV.

.. image:: images/37-registered-validator.png
   :alt: Registered validator

Observability Dashboard
-----------------------

.. note:: Observability may no longer work while App Quickstart is under revisions.

In a web browser, navigate to http://localhost:3030/dashboards to view
the observability dashboards. Select “Quickstart - consolidated logs”.

.. image:: images/38-obs-dash.png
   :alt: observability dashboard

The default view shows a running stream of all services.

.. image:: images/39-service-stream.png
   :alt: service stream

Change the services filter from “All” to “participant” to view participant logs.
Select any log entry to view its details.

.. image:: images/40-log-entry-details.png
   :alt: log entry details

SV UIs
------

Navigate to http://sv.localhost:4000/ for the SV Web UI.
The SV view displays data directly from the validator in a GUI that is straightforward to navigate.

Login as ‘administrator’.

.. image:: images/33-sv-ui-login.png
   :alt: SV UI login

The UI shows information about the SV and lists the active SVs.

.. image:: images/34-active-svs.png
   :alt: Active SVs

The Validator Onboarding menu allows for the creation of validator onboarding secrets.

.. image:: images/35-validator-onboarding.png
   :alt: Validator onboarding

Next steps
==========

You’ve completed a business operation in the CN App Quickstart and have been introduced to the basics of the Canton Console and Daml Shell.

Learn more about Daml Shell and the project structure in the :ref:`Project Structure guide<project-structure-overview>`.
