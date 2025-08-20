The Canton Quickstart example application
=========================================

Business case
-------------

The Quickstart features a sample licensing app to demonstrate Canton development patterns. 
In the app, providers sell time-based access to their services.
Users pay with Canton Coin (CC) and manage payments through a Canton Wallet. 

The app involves four parties:

- The **Application Provider** who sells licenses.
- The **Application User** who buys licenses.
- The underlying **Amulet** token system that handles payments, using `Canton Coin <https://www.canton.network/blog/canton-coin-a-canton-network-native-payment-application>`__ by default.
- The **DSO Party**, the Decentralized Synchronizer Operations Party who operates the Amulet payment system. In CN, this is the Super Validators.

Core business operations
------------------------

Issuing a license
~~~~~~~~~~~~~~~~~

The provider creates a new license for an onboarded user. 
The license starts expired and needs to be renewed before use.

Requesting a license renewal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The provider creates a renewal request, which generates a payment request for the user.
The user sees the renewal offer and the payment amount. 
A matching CC payment request is created on the ledger.

Paying for a license renewal
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The user approves the the payment through their Canton Wallet, which creates an accepted payment contract on the ledger.

Renewing the license
~~~~~~~~~~~~~~~~~~~~

The provider processes the accepted payment and updates the license with a new expiration date.
