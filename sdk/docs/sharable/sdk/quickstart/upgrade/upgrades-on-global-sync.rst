

.. _upgrades-on-the-global-synchronizer:

.. todo: https://github.com/digital-asset/cn-quickstart/issues/144 -- determine whether and what to keep

Upgrades on the Global Synchronizer
===================================

**Contents**

`Upgrades on the Global Synchronizer <#upgrades-on-the-global-synchronizer>`__

   `Type 1: backward-compatible changes <#type-1-backward-compatible-changes>`__

   `Type 2: Daml model changes <#type-2-daml-model-changes>`__

   `Type 3: non-compatible protocol changes <#type-3-non-compatible-protocol-changes>`__

   `Preparing for upgrades <#preparing-for-upgrades>`__

The SVs periodically implement upgrades to the Global Synchronizer to improve functionality, resolve issues, and introduce new features.
As a node operator or application provider you should be aware of the three types of upgrades that may occur.

Type 1: backward-compatible changes
-----------------------------------

Type 1 upgrades involve backward-compatible changes to the Splice applications and/or modifications to the behavior of the Canton synchronization layer.
These non-breaking changes occur on Mondays, every week.

While validators can operate effectively when behind by a Splice version or two, the SVs recommend keeping your node up to date with weekly upgrades.
It's worth noting that "skip upgrades" (jumping multiple versions at once) are not officially tested by the SVs, so while they generally work, they come with increased risk.

Type 2: Daml model changes
--------------------------

Type 2 upgrades modify the Daml models that underlie the Splice applications.
These changes introduce a fork in the application chains and occur every few months.

The process for Type 2 upgrades begins with distribution of the new Daml models through Type 1 upgrades, followed by an offline Canton Improvement Proposal (CIP) that must be approved by the SV node owners.
Next, the SVs conduct an onchain vote to establish a specific date and time when the new models take effect.
At this cutoff point, only validators running the most recent Splice version are able to participate in transactions using the new models.
Validators that haven't adopted the latest version are unable to participate in transactions.

Type 3: non-compatible protocol changes
---------------------------------------

Type 3 upgrades involve fundamental changes to the Canton synchronization protocol.
These major upgrades require downtime (sometimes called Hard Migrations) and occur every three to four months.

The implementation of Type 3 upgrades requires a Canton Improvement Proposal (CIP) approved through an offchain vote, followed by an onchain vote by the SVs to schedule the upgrade.
These migrations impact all SVs and Validators, requiring a coordinated transition from the prior protocol to the new one.
Currently, Canton requires all nodes to migrate together during these upgrades.

Preparing for upgrades
----------------------

Application providers should maintain nodes on DevNet, TestNet, and MainNet to guarantee smooth operations during upgrades.
By maintaining nodes across all three environments you substantially increase the likelihood that MainNet upgrades proceed without disrupting your services or customers.
