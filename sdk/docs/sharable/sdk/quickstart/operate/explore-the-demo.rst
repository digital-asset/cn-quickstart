.. _quickstart-explore-the-demo:

======================================================
Explore the Canton Network Application Quickstart demo
======================================================

.. contents:: Contents
   :depth: 2
   :local:
   :backlinks: top

Business case
=============

The Canton Network (CN) Quickstart is scaffolding to support development efforts to build, test, and deploy CN applications.
It resolves infrastructure problems that every CN application must solve.
Use the CN Quickstart Application so you and your team can focus on building your application, instead of build systems, deployment configurations, and testing infrastructure.

Core business operations
------------------------

The Quickstart features a sample licensing app to demonstrate Canton development patterns. 
In the app, providers sell time-based access to their services.
Users pay with Canton Coin (CC) and manage payments through a Canton Wallet. 

The app involves four parties:

- The **Application Provider** who sells licenses.
- The **Application User** who buys licenses.
- The underlying **Amulet** token system that handles payments, using `CC <https://www.canton.network/blog/canton-coin-a-canton-network-native-payment-application>`__.
- The **DSO Party**, the Decentralized Synchronizer Operations Party who operates the Amulet payment system. In CN, this is the Super Validators.

The application issues licenses using the following process:

Issuing a license
~~~~~~~~~~~~~~~~~

The provider creates a new license for an onboarded user. 
The license starts expired and needs to be renewed before use.

Requesting a license renewal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The provider creates a renewal request, generating a payment request for the user.
A matching CC payment request is created on the ledger.

Paying for a license renewal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The user approves the payment through their Canton Wallet, which creates an accepted payment contract on the ledger.

Renewing the license
~~~~~~~~~~~~~~~~~~~~

The provider processes the accepted payment and updates the license with a new expiration date.

Overview
========

This How-to helps you become familiar with a Canton Network (CN) business operation within the CN App Quickstart.
The App Quickstart application is intended to be extended by your team to meet your business needs.
When you are familiar with the App Quickstart, review the technology choices and application design to determine what changes are needed.
Technology and design decisions are ultimately up to you.

If you find errors, please contact your representative at Digital Asset.

Prerequisites
=============

Install the `CN App Quickstart <../download/cnqs-installation.html>`__ before beginning this demonstration.

Walkthrough
===========

The CN App Quickstart can run with or without authorization, based on your business needs.
Toggle authorization with the ``make setup`` command in the ``quickstart`` subdirectory.
``make setup`` asks to enable Observability, OAUTH2, and specify a party hint.
In this demo, we disable ``TEST MODE``, use the default party hint, and show OAUTH2 as enabled and disabled.
When OAUTH2 makes a difference, we display both paths, one after the other. 
You can follow your path and ignore the other.
You may enable Observability, but it is not required for this demo.

**Choose your adventure:**

``make setup`` **without** OAUTH2:

.. image:: images/make-setup-noauth.png
   :alt: Make setup no auth

``make setup`` **with** OAUTH2:

.. image:: images/make-setup-with-oauth.png
   :alt: Make setup with auth

Build Quickstart
----------------

.. youtube:: xsuMDLED6gI

Build and start App Quickstart:

.. code-block:: bash
   
   make build; make start

Open an incognito browser and navigate to:

::

   app-provider.localhost:3000

Alternatively, in the terminal, from quickstart/ run:

::

  ``make open-app-ui``

.. note:: Safari users may need to manually map the ``app-provider`` subdomain in ``/etc/hosts``.
   Use the terminal command ``sudo nano /etc/hosts`` to add:
   
   ``127.0.0.1       app-provider.localhost``
   
   This tells your system to resolve ``app-provider.localhost`` to your local machine.
   Then save and close the file. 
   Restart Safari.

Login
-----

**OAUTH2 disabled**

When OAUTH2 is **disabled**, the homepage presents a simple login field.
Begin by logging in as the ``AppProvider`` by entering "app-provider" in the User field.

.. image:: images/01-login-app-qs-noauth.png
   :alt: CN App Quickstart Login screen without Auth
   :width: 60%

**OAUTH2 enabled**

When OAUTH2 is **enabled**, the homepage prompts to login with Keycloak's OAuth 2.0 portal:

.. image:: images/01-login-app-qs-auth.png
   :alt: CN App Quickstart Login screen with Auth
   :width: 60%

Make a mental note that ``AppProvider``’s username is “app-provider” and the password is "abc123" (all lowercase).

Login with ``app-provider`` with keycloak.

Fill in the login credentials: username: app-provider, password: abc123

.. image:: images/login-app-provider-view.png
   :alt: AppProvider login screen
   :width: 60%

The App Installs Menu
---------------------

Once you are logged in select **AppInstalls** in the menu.

.. image:: images/qs-demo-app-installs-view.png
   :alt: App Installs view

Open a terminal to create an app install request.

From ``/quickstart/`` run:

::

  make create-app-install-request

This command creates an App Installation Request on behalf of the Participant.

.. image:: images/04-create-install-req.png
   :alt: App Install Request

.. note:: If your machine is not powerful enough to host ``LocalNet`` or if the docker containers are not responsive then the response may show a failure with status code 404 or 000 (as shown in the image below). Increasing Docker memory limit to at least 8 GB should allow the ``LocalNet`` containers to operate properly.

.. image:: images/05-error-app-install.png
   :alt: App Install Request error

Return to the browser.

AppInstallRequest
-----------------

The install request appears in the list.

Click **Accept**.

.. image:: images/accept-awaiting-request.png
   :alt: accept request

The ``AppInstallRequest`` is Accepted. 

.. image:: images/success-accepted-appinstallrequest.png
   :alt: accepted request
   :width: 60%

The actions update to Cancel and Create license.

Create a license
----------------

Click **Create License**.
The license is created and the “# Licenses” field is updated.

.. image:: images/created-license.png
   :alt: create license

Next, navigate to the Licenses menu and select **Renewals**.

.. image:: images/new-license-select-renewals.png
   :alt: Licenses view

A "License Renewal Request” modal opens with an option to renew a license.

.. image:: images/license-renewal-request-modal.png
   :alt: license renewal request modal

Click **New** to open the "Renew License" modal.

.. image:: images/renew-license-modal.png
   :alt: renew license modal

In the modal, set the number of days to renew the license, the fee, time to prepare the license, and time to settle the license.
You must add a description to proceed.

"Prepare in" is an indication for the sender (app-user) that they are expected to accept allocation before that time.
"Settle in" is a time that the provider has to ``completeRenewal``. 
After that, the allocation will be expired.

Click **Issue License Renewal Request**.

.. image:: images/new-license-renewal-request.png
   :alt: new license renewal request

Per the Daml contract, licenses are created in an expired state.
To activate the license, a renewal payment request must be issued.

Make a payment
--------------

To make payment, navigate to the Canton Wallet at http://wallet.localhost:2000/allocations and log in as ``app-user`` if prompted.

You can find the wallet's location by: 

1. Reading the `Splice Local Network docs <https://docs.dev.sync.global/app_dev/testing/localnet.html#application-uis>`__.
2. Navigating to the App Provider's "Tenants" menu.

.. image:: images/app-provider-tenants.png
   :alt: AppProvider Tenants menu

3. Logging into the app as ``app-user``, navigating to the Licenses menu, then clicking the **Renewals** action.

.. image:: images/app-user-licenses-menu.png
   :alt: AppUser Licenses menu

If prompted, log in to the CC Wallet as ``app-user``.

.. image:: images/canton-coin-wallet-app-user-log-in.png
   :alt: Canton Coin Wallet login
   :width: 70%

If your wallet does not have CC then enter an amount and click **TAP**.
After a moment, the available balance will automatically update.

.. image:: images/tap-canton-wallet.png
   :alt: Tap for CC

Once your CC wallet is loaded, navigate to the "Allocations" menu and accept the "Allocation Request" before the "Allocate before" time expires.

.. image:: images/canton-coin-wallet-allocations-menu.png
   :alt: CC Wallet accept allocation

If the allocation request is accepted, a new "Allocations" section appears.
This section shows the ``licenseFeePayment`` information.

.. image:: images/canton-coin-wallet-accepted-allocation.png
   :alt: CC Wallet accepted allocation

Renew the license
-----------------

Return to the Quickstart as the ``AppProvider``.
In the Licenses menu, select **Renewals**.
This opens the License Renewals Request modal. 
Click the green **Complete Renewal** button.

.. image:: images/app-provider-complete-renewal-after-payment.png
   :alt: complete renewal after payment

A confirmation appears that the license renewal completed successfully.

.. image:: images/license-renewal-completed-successfully.png
   :alt: renewal success after payment
   :width: 60%

Log out from the ``AppProvider`` and log in as ``AppUser``.

**OAUTH2 disabled**

If OAUTH2 is disabled, simply log in as ``app-user``.

.. image:: images/login-app-user-noauth.png
   :alt: AppUser login screen without Auth
   :width: 40%

**OAUTH2 enabled**

When OAUTH2 is enabled, you log in using the app-user username and password.

.. image:: images/01-login-app-qs-auth.png
   :alt: login screen
   :width: 60%

Login as ``AppUser`` with “app-user" as the username and the password is “abc123”.

.. image:: images/appuser-auth-login-view.png
   :alt: AppUser login screen
   :width: 60%

The AppInstall now shows as accepted.

.. image:: images/accepted-app-install.png
   :alt: accepted AppInstall

The license shows as active.

.. image:: images/app-user-license-active.png
   :alt: logout AppProvider

Congratulations. You’ve successfully created and activated a license with a payment allocation in Canton wallet!

Canton Console
==============

.. youtube:: zADHja_8TSg

The :externalref:`Canton Console <canton_console>` connects to the running application ledger.
The console allows a developer to bypass the UI to interact with the CN in a more direct manner.
For example, in Canton Console you can connect to the Participant to see the location of the Participant and their synchronizer domain.

Activate the :externalref:`Canton Console <canton_remote_console>` in a terminal from the ``quickstart/`` directory.
Run:

::

  make canton-console

After the console initiates, run the ``participants`` and ``participants.all`` commands, respectively.

::

  participants

Returns a detailed categorization of participants.

.. image:: images/canton-console-participants.png
   :alt: Participant location in the ledger

::

  participants.all

Shows a list of all participant references.

.. image:: images/canton-console-participants-all.png
   :alt: Participant synchronizer

On ``LocalNet``, you can connect to any of the listed participants.
Connect to the app user's validator with 

::
   
   `app-user`

.. image:: images/app-user.png
   :alt: App User

If you receive an error, double check that you used the backticks.

The app provider can be connected with: 

::

   `app-provider`

.. image:: images/app-provider.png
   :alt: App Provider

Connect to the Super Valdiator that is simulating the Global Synchronizer using:

::

   `sv`

.. image:: images/sv.png
   :alt: super validator

Canton Console also provides a diagnostic tool that displays the health of Canton Network validators:

::

  health.status

.. image:: images/health-status.png
   :alt: Ping yourself

Daml Shell
==========

.. youtube:: bwUyYEFCo5w

The :externalref:`Daml Shell <build_daml_shell_component_howto>` connects to the running PQS database of the application provider’s Participant.
In the Shell, the assets and their details are available in real time.

Run the shell from quickstart/ in the terminal with:

::

  make shell

Run the following commands to see the data:

::

  active

Shows unique identifiers and the asset count:

.. image:: images/28-shell-ids.png
   :alt: Active identifiers
   :width: 90%

::

  active quickstart-licensing:Licensing.License:License

List the license details.

.. image:: images/29-license-details.png
   :alt: License details

::

  active quickstart-licensing:Licensing.License:LicenseRenewalRequest

Displays license renewal request details.

.. image:: images/active-quickstart-appinstallrequest.png
   :alt: License renewal request details

::

  archives quickstart-licensing:Licensing.AppInstall:AppInstallRequest

Shows any archived license(s).

.. image:: images/30-archive-licenses.png
   :alt: Archived licenses

Canton Coin Scan
================

Explore the CC Scan Web UI at http://scan.localhost:4000/.


The default activity view shows the total CC balance and the Validator rewards.

.. image:: images/36-cc-balance.png
   :alt: CC balance
   :width: 70%

Select the **Network Info** menu to view SV identification.

.. image:: images/34-active-svs.png
   :alt: Active SVs

The Validators menu shows that the local validator has been registered with the SV.

.. image:: images/37-registered-validator.png
   :alt: Registered validator
   :width: 80%

Observability Dashboard
=======================

.. note:: Observability may no longer work while App Quickstart is under revisions.

In a web browser, navigate to http://localhost:3030/dashboards to view
the observability dashboards. Select **Quickstart - consolidated logs**.

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
======

Navigate to http://sv.localhost:4000/ for the SV Web UI.
The SV view displays data directly from the validator in a GUI that is straightforward to navigate.

Login as ‘sv’.

.. image:: images/33-sv-ui-login.png
   :alt: SV UI login
   :width: 80%

The UI shows information about the SV and lists the active SVs.

.. image:: images/34-active-svs.png
   :alt: Active SVs

The Validator Onboarding menu allows for the creation of validator onboarding secrets.

.. image:: images/35-validator-onboarding.png
   :alt: Validator onboarding

Next steps
==========

You’ve completed a business operation in the CN App Quickstart and have been introduced to the basics of the Canton Console and Daml Shell.
We encourage you to explore the CN App Quickstart codebase and modify it to meet your business needs.
You might be interested in learning more about the App Quickstart :ref:`quickstart-project-structure-guide` or the :ref:`quickstart-development-journey-lifecycle`.