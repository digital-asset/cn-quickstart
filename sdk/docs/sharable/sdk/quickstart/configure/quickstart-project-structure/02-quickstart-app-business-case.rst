The Canton Quickstart example application
=========================================

Business case
-------------

The Canton Quickstart contains an example application providing both a
demonstration of a Canton application targeted at production; and, a way
of exercising the supporting developer scaffolding provided by the
bootstrap.

This example application is a simple license management application that
allows the application provider to issue licenses to application users.
Canton Coin is used in this transaction. These users are assumed to be
potentially retail customers of the provider, with access to Canton and
a Canton Wallet, but not necessarily running their own infrastructure
beyond a (possibly outsourced) validator node.

The relevant business entities are:

**Amulet**: An infrastructure token usable on the Canton synchronizer
being used by this application. In the case of an application using
Canton Network, this will be Canton Coin [4]_.

**DSO Party:** The Decentralized Synchronizer Operations Canton Party.
This is the party that operates the Amulet token in which the provider
accepts license payments. In the case of a Canton Network Application,
this will be the Global Synchronizer Foundation.

**Application Provider**: This is a Canton Party representing the legal
entity deploying, running, and offering the application to their users
(customers). In a licensing application, this is the entity offering to
sell the licenses.

**Application User**: This is a Canton Party representing the legal
entity that is (presumably) a customer of the application provider. In
this application this is an entity with a need to purchase a license,
and periodically renew it. Canton Coin is exchanged for the license.

Core Workflows Happy Path Business Requirement
----------------------------------------------

Issuing a License
~~~~~~~~~~~~~~~~~

*Given* an application user (app-user) has been onboarded to the
licensing application

*When* the application provider (app-provider) instructs the application
to create a new license for the app-user

*Then* a new expired license will be created on the ledger and made
visible to the app-user

Requesting a License Renewal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Given* an app-user has a license (l1)

*And* the current datetime is greater than the expiration date on
license (l1)

*When* the app-provider instructs the application to request a license
renewal

*Then* a license renewal will be created and made visible to the user

*And* a matching amulet (canton coin) payment request will be created on
the ledger

Paying for a License Renewal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Given* there is a license renewal request on the ledger

*And* a matching amulet payment request on the ledger

*When* the user indicates they wish to pay the renewal

*Then* the user will be redirected to the payment request in their
wallet, and can approve the request

Renewing the license
~~~~~~~~~~~~~~~~~~~~

*Given* an app-user has approved an amulet payment request associated
with a license renewal request

*And* there is an AcceptedAppPayment contract (accepted-payment) on the
ledger corresponding to that approval

*When* the app-provider instructs the application to complete the
renewal transaction

*Then* the license will be updated with a new expiration date = renewal
duration + max (old expiration date, now)

*And* the app-provider will exercise the AcceptedAppPayment_Collect
choice on accepted-payment

.. [4]
   https://www.canton.network/blog/canton-coin-a-canton-network-native-payment-application