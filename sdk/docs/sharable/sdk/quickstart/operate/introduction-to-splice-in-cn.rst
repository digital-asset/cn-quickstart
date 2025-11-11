.. _quickstart-splice-in-cn:

.. todo: https://github.com/digital-asset/cn-quickstart/issues/144 -- determine whether and what to keep

============================================
Introduction to Splice in the Canton Network
============================================

Overview
--------

Splice is a collection of reference applications that allow entities to
operate, fund, and govern publicly available decentralized Canton
synchronizers. It presents a reference method for operating
decentralized Canton synchronizers through entities known as Super
Validators (SVs). Within the Canton Network (CN), Splice provides the
economic infrastructure and governance mechanisms that support
decentralized synchronizers to function sustainably. Its applications
include payment utilities, rewards systems, and governance tools that
create a transparent framework for operating the network.

DevNet in the Canton Network
----------------------------

DevNet serves as the shared development and testing environment for the
CN where applications built with Splice and Canton components can
interact with the actual network infrastructure. After initial
development in
`LocalNet <https://docs.google.com/document/d/1pQFlntz2T71KCJo5W3DF-wOiKc-hQRFANGcLiKC5mXo/edit?tab=t.0#heading=h.l71fgql0zly3>`__,
developers connect to DevNet to validate their applications against real
network protocols, including communication with SVs running Splice
components. The Canton Network Quickstart (CN QS) provides a
configurable environment that allows developers to test their
applications in a controlled but realistic network environment before
moving to production deployments.

What is Splice?
---------------

Splice, maintained by `Hyperledger
Labs <https://github.com/hyperledger-labs/splice>`__, provides
infrastructure for entities to jointly operate and fund Canton
synchronizers. It offers components and implementations that demonstrate
how to create economic and governance systems that facilitate network
operations. Splice provides a framework that helps multiple independent
operators make collective decisions, process payments, and maintain
shared infrastructure.

How Splice is used in the Canton Network
----------------------------------------

Splice is deployed on the Global Synchronizer. Its components
include Amulet, a payment utility token, commonly referred to as Canton
Coin (CC), that services financial transactions between network
participants. Supporting services include the Amulet Name Service (ANS),
a Traffic Acquisition Program (TAP), and payment scan functionality.
These tools support SVs to coordinate with one another while managing
nodes. They create a transparent economic ecosystem that supports the
ongoing funding and operation of decentralized Canton synchronizers.

Division of responsibilities between Splice and Canton Components
-----------------------------------------------------------------

Canton components handle core transaction processing, privacy, and
consensus functionality across the network. Splice focuses on the
operational layer of running decentralized synchronizers. Nodes in the
decentralized synchronizer are operated by SVs. A group of SVs actively
operating nodes in a decentralized synchronizer are referred to as the
"Decentralized Synchronizer Operator" (DSO). Splice uses a code
construct called a "Decentralized Synchronizer Operator Party" (DSO
Party) to collect signatures for joint actions, maintain synchronization
infrastructure, and implement economic policies.

Benefits of Splice in the CN QS
-------------------------------

Splice provides the CN with a sustainable funding model for
synchronization services, transparent operations governed by SVs, and
standardized payment methods for transaction fees. Splice’s integration
into the CN infrastructure creates accessibility for CN developers
within a cohesive environment that securely facilitates applications to
interact with synchronizers.

Technical overview of Splice in the CN QS
-----------------------------------------

In CN QS, the Splice and Canton components operate in separate Java
Virtual Machines (JVMs). Although this architecture is still in
development, it is designed to preserve logical separation between
validation and execution, optimize resource allocation for each
component, maintain clear security boundaries, and provide independent
scaling and configuration. Splice validators connect to Canton
participants through well-defined APIs that creates a cohesive system
that balances separation of concerns with operational integration.
