Application structure
---------------------

Example application architecture
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is tempting to see three layers and immediately assume these align
with the traditional 3-tier architecture (User Interface, Business
Logic, Database), but doing this will result in underperforming
applications generating unnecessary traffic on the Global Synchronizer.
It is easy to fall into the trap to treat the blockchain as a database
because it has very similar features to a database. However, applying
standard database design techniques to a blockchain does not produce an
optimal design. A better way to view these layers is in terms of
consensus vs. local state. Specifically: User Interface, Local Business
Logic and State, and Consensus Business Logic and State.

-  Local Business Logic and State: Actions and data that a single
   participant node can handle on their own without needing consensus
   from others.

-  Consensus Business Logic and State: Actions and data that require
   agreement or validation from multiple parties and need to be handled
   using Daml smart contracts.

+-------------------+-------------------------+------------------------+
| Frontend          | **User Interface**      |                        |
+-------------------+-------------------------+------------------------+
|                   | HTTP/JSON               |                        |
+-------------------+-------------------------+------------------------+
| Backend Services  | **Local Business Logic  |                        |
|                   | and State**             |                        |
+-------------------+-------------------------+------------------------+
|                   | GRPC/Protobuf           | HTTP/JSON              |
+-------------------+-------------------------+------------------------+
| Daml Models       | **Consensus Business    |                        |
|                   | Logic and State**       |                        |
+-------------------+-------------------------+------------------------+

A symptom that you have fallen into the trap of treating the blockchain
like a database is a prevalence of CRUD operations in the web-services
provided by the backend. The blockchain is intended to synchronize data
between multiple organizations with little trust between them. This
means that the operations between organizations should be at a larger
granularity, invariably representing business operations rather than
updates to an object store\ *.*

The privacy guarantees provided by Canton do not exist on a publicly
visible ledger. So all business logic and business state that need to be
either authorized or verified by more than one party is implemented
within the Daml smart contract. The necessary consensus on
authorization, verification, and/or visibility will then be coordinated
via the (Global) Synchronizer. For a more detailed discussion on the
distinction between local vs. consensus logic and state see the Daml
Philosophy Course 2 “Daml Workflows” [19]_.

Alternative Application Architecture
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This example application could have used a CQRS-style alternative
architecture. This architecture is often used where front end user
action stories are expressed directly in terms of unmediated consensus
business operations. This means:

-  User interface updates (writes) are performed directly against the
   Daml models rather than mediated through backend services.

-  User interface queries (reads) remain provided by backend services;
   which also continue to provide external integrations and automation.

+----------------+----------------------+-----------+-----------------+
| Frontend       | **User Interface**   |           |                 |
+----------------+----------------------+-----------+-----------------+
|                | **HTTP/JSON**        |           | **Ledger Update |
|                |                      |           | Operations**    |
+----------------+----------------------+-----------+-----------------+
| Backend        | **Queries,           |           |                 |
| Services       | Automation and       |           |                 |
|                | Integration**        |           |                 |
+----------------+----------------------+-----------+-----------------+
|                | **GRPC/Protobuf**    | **HTTP    |                 |
|                |                      | /JSON**   |                 |
+----------------+----------------------+-----------+-----------------+
| Daml Models    | **Consensus Business |           |                 |
|                | Logic and State**    |           |                 |
+----------------+----------------------+-----------+-----------------+

For a detailed discussion on options for application architectures see
the free courses in the Technical Solution Architect
Certification [20]_.

Daml Model Structure
--------------------

.. code-block:: text

   √ % tree licensing
   licensing
   ├── daml
   │  └── Licensing
   │  ├── AppInstall.daml
   │  ├── License.daml
   │  └── Util.daml
   └── daml.yaml
   3 directories, 4 files

The example application is a simple license management application that
allows the application provider to issue licenses to application users;
with license fees paid using Canton Coin. It uses a Daml model
consisting of two modules. The `AppInstall` module has two
responsibilities:

1. The on-ledger component of user onboarding using the
   AppInstallRequest template

2. The core services provided to each onboarded user through the
   application using the AppInstall template

For the purposes of testing and experimentation there is a make
target [21]_ to create the `AppInstallRequest` on behalf of the app user
party.

.. code-block:: text

   .PHONY: create-app-install-request
   create-app-install-request: ## Submit an App Install Request from the App User participant node
   docker compose -f docker/app-user-shell/compose.yaml \
   $(DOCKER_COMPOSE_ENVFILE) run --rm create-app-install-request || true

This uses curl via a utility function curl_check [22]_ to submit a Daml
Create command to Org1’s participant node via its HTTP Ledger JSON API
(`v2/commands/submit-and-wait`).

.. code-block:: text

   √ % cat docker/app-user-shell/scripts/create-app-install-request.sh
    #!/bin/bash
    ...
    source /app/utils.sh

    create_app_install_request() {
        curl_check "http://$participant:7575/v2/commands/submit-and-wait" \
        "$token" "application/json" \
        --data-raw '{
            "commands" : [
                { "CreateCommand" : {
                    "template_id": "#quickstart-licensing:Licensing.App Install:AppInstallRequest",
                    "create_arguments": {
                        "dso": "'$dsoParty'",
                        "provider": "'$appProviderParty'",
                        "user": "'$appUserParty'",
                        "meta": {"values": []}
                    }
                } }
            ]
        }'
    }

   create_app_install_request "$LEDGER_API_ADMIN_USER_TOKEN_APP_USER" \
   $DSO_PARTY $APP_USER_PARTY $APP_PROVIDER_PARTY participant-app-user

Running this and then using `Daml
Shell <https://docs.daml.com/tools/daml-shell/index.html#daml-shell-daml-shell>`__\  [23]_
(make shell provides a useful shortcut) to inspect the result on the
ledger.

.. code-block:: text

   √ % make shell
    docker compose -f docker/daml-shell/compose.yaml --env-file .env run \
    --rm daml-shell || true
    Connecting to
    jdbc:postgresql://postgres-splice-app-provider:5432/scribe...
    Connected to
    jdbc:postgresql://postgres-splice-app-provider:5432/scribe
    postgres-splice-app-provider:5432/scribe> active
    ┌─────────────────────────────────────────────────────────────┬──────────┬───────┐
    │ Identifier                                                  │ Type     │ Count │
    ╞═════════════════════════════════════════════════════════════╪══════════╪═══════╡
    │ quickstart-licensing:Licensing.AppInstall:AppInstallRequest │ Template │   1   │
    ├─────────────────────────────────────────────────────────────┼──────────┼───────┤
    │ splice-amulet:Splice.Amulet:ValidatorRight                  │ Template │   1   │
    ├─────────────────────────────────────────────────────────────┼──────────┼───────┤
    │ splice-wallet:Splice.Wallet.Install:WalletAppInstall        │ Template │   1   │
    └─────────────────────────────────────────────────────────────┴──────────┴───────┘
    postgres-splice-app-provider:5432/scribe 3f → 42> active
    quickstart-licensing:Licensing.AppInstall:AppInstallRequest
    ┌─────────┬──────────┬───────────┬───────────────────────────────────────────────┐
    │ Created │ Contract │ Contract  │ Payload                                       │
    │ at      │ ID       │ Key       │                                               │
    ╞═════════╪══════════╪═══════════╪═══════════════════════════════════════════════╡
    │ 42      │ 0058df2  │           │ dso: DSO: :1220c93d1...                       │
    │         │ 3a5aaa4  │           │ meta:                                         │
    │         │ c2a53a...│           │   values:                                     │
    │         │          │           │ user: Org1: :12203a9a7...                     |
    │         │          │           │ provider: AppProvider: :122030b08cfebb8c8...  │
    └─────────┴──────────┴───────────┴───────────────────────────────────────────────┘
    postgres-splice-app-provider:5432/scribe 3f → 42> contract
    0058df23a5aaa4c2a53aab496d12fb9e8ee74fb91614e5f7d50670598e4760eb23ca101220cc241620b310c93af45b2cd7cea7518e18e26f73f227813fec2bf4ea0bd69b940120cc241620b310c93af45b2cd7cea7518e18e26f73f227813fec2bf4ea0bd69b94
    ╓───────────────────────╥─────────────────────────────────────────────────────────────╖
    │ Identifier            ║ quickstart-licensing:Licensing.AppInstall:AppInstallRequest ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    │ Type                  ║ Template                                                    ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    │ Created at            ║ 42 (not yet active)                                         ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    │ Archived at           ║ <active>                                                    ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    │ Contract ID           ║ 0058df23a5aaa4c2a53a...                                     ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    │ Event ID              ║ #12201612fb8a071e27ec...:0                                  ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    │ Contract Key          ║ <not set>                                                   ║
    ╟───────────────────────╫─────────────────────────────────────────────────────────────╢
    | Payload               ║ dso: DSO: :1220c93d13220b07f0e9a0a0f7a2381191d3bf3d21...    │
    |                       ║ meta:                                                       │
    |                       ║   values:                                                   │
    |                       ║ user: Org1: :12203a9a79d8f72b8cce37813713af7a51296def8...   │
    |                       ║ provider: AppProvider: :122030b08cfebb8c87c16793cba3783...  │
    ╚═══════════════════════╩═════════════════════════════════════════════════════════════╝
    postgres-splice-app-provider:5432/scribe 3f → 42>

Exercising the `AppInstallRequest_Accept` choice completes the onboarding.
The Frontend UI provides a way to do this.

Key Daml Templates
~~~~~~~~~~~~~~~~~~

AppInstallRequest Contract
^^^^^^^^^^^^^^^^^^^^^^^^^^

The `AppInstallRequest` contract initiates the app user onboarding process
by capturing a user’s request to install the application. The contract
gives the application provider (henceforth just *provider*) control over
application access to accept or reject installation requests. This
contract offers three choices that extend the Propose/Accept
pattern [24]_ to allow the user to cancel the request.

The `AppInstallRequest_Accept` choice allows the provider to accept the
request. When the choice is executed, it creates a new AppInstall
contract and makes the provider and user signatories.

The `AppInstallRequest_Reject` choice allows the provider to decline the
request. It archives the request contract and also records in the ledger
exercise event, metadata about why the request was rejected.

The `AppInstallRequest_Cancel` choice allows the user to withdraw their
request any time before the provider accepts the contract.

AppInstall Contract
^^^^^^^^^^^^^^^^^^^

The `AppInstall` contract maintains the formal relationship between the
provider and user. It tracks installation status and manages license
creation. The contract has two choices, `AppInstall_CreateLicense` and
`AppInstall_Cancel`.

`AppInstall_CreateLicense` allows the provider to create a new license for
the user. When the `CreateLicense` choice is exercised it creates a new
License contract. It also increments `numLicensesCreated` to track how
many licenses exist which is used to assign each licence a licence
number. **Note:** Daml smart contracts are immutable, so “incrementing”
the counter results in archiving the current `AppInstall` contract and
creating a new one with the updated counter, within the same atomic
transaction.

`AppInstall_Cancel` lets the provider or user cancel the installation.

License Contract
^^^^^^^^^^^^^^^^

The `License` contract is the on ledger record supporting the core
business case for the application. One critical field is the `expiresAt`
field, which both determines the duration of the license’s validity, and
is used to ensure that neither actor can revoke (ie. archive) the
license contract before expiry. The contract also has two choices:

`License_Renew` can be exercised by the license provider. It creates a
Splice [25]_ `AppPaymentRequest` and a `LicenseRenewalRequest` contract. The
former is a part of the Splice Wallet Application, and is used to
request an amulet transfer. The choice of which amulet is made via the
dso party used in the `AppInstall` contract. The current deployment
configuration will result in this being Canton Coin; however, there is
nothing in the Daml model, or the backend code that prevents a different
amulet being used.

The `License_Expire` choice allows either party to archive an expired
`License` contract. This has the benefit of allowing an expired license to
be renewed without having to reissue it. It is also necessary because
Daml smart contracts do not have any facility to self-execute or
self-archive. Every change to the ledger originates from a command
submitted to the ledger API on a validator. As a result this sort of
cleanup operation must be exercised explicitly via a choice such as
this. It is not uncommon to have background or end-of-day batch
processes automate this sort of task.

Common OpenAPI Definition
-------------------------

The Daml models define the consensus between the App Provider, App User,
and the DSO (amulet issuer). Once the models are in use, the front end
user interface needs to be able to query and interact with the resulting
ledger. The usual pattern is to store and index the relevant slice of
the ledger in the `Participant Query
Store <https://docs.daml.com/query/pqs-user-guide.html#pqs>`__\  [26]_,
and provide a set of query web services that provide business oriented
queries resolved against the PQS postgres database.

The architecture used by the example application also exposes a variety
of HTTP endpoints that allow the frontend to exercise choices, providing
a bridge between the frontend and the GRPC Ledger API. This allows the
backend to centralise authentication and access control code.

This does necessitate defining an API between the back and front ends.
For this example application, we have chosen to use OpenAPI [27]_. The
API definition is in `common/openapi.yaml`. It uses GET to access the
query services in the backend; and, POST to execute choices on contracts
identified by contract-id in the URL.

**Note:** This is using HTTP. The HTTP method semantics align
appropriately with the requirements of the Daml operations and we call
this a “JSON API”. However, it is not a pure ReST [28]_ API and does use
HATEOAS. As mentioned above, the blockchain should not be viewed as a
database since the underlying state is not rows in a database, or
objects in a datastore—either of which would be compatible with the
CRUD-style semantics that emerge with most modern ReST tooling. Instead
the architecture style used here is more akin to a sophisticated RPC
mechanism [29]_.

Backend Services Structure
--------------------------

The example backend is a SpringBoot [30]_ application the core of which
are the API implementation classes in
com.digitalasset.quickstart.service.

Most of this code is standard Java SQL-backed JSON-encoded HTTP web
service fare. The code itself is divided into seven modules under
com.digitalasset.quickstart.*:

`config`: Mostly standard SpringBoot @ConfigurationProperties based
components; however, SecurityConfig may be worth looking at for how the
example application handles CSRF tokens and OAuth2 authentication of
login and logout requests.

`oauth`: Amongst other things, provides a client interceptor to
authenticate the backend services to the Ledger API.

`service`: Implements the openAPI endpoints. Mostly a roughly equal split
between read-only calls to PQS via the DamlRepository spring component;
and, GRPC calls to the relevant validator via the LedgerApi spring
component.

`ledger`: The main class here is `LedgerApi` which handles the details of
calling the relevant GRPC endpoints required to submit Daml commands and
other requests to the Canton Validator.

`repository`: Includes \`DamlRepository`. A `@Repository` component
providing business-logic level query and retrieval facilities against
the ledger via PQS (the Participant Query Store).

`pqs`: The main class is `Pqs`, which provides data-model level query and
retrieval. This encapsulates the necessary SQL generation, and the JDBC
queries against the PQS Postgres database.

`utility`: For the moment this is restricted to the `ObjectMapper` required
for JSON transcoding in the web services.

Ultimately the main recommendation embedded in this code is to orient
the web-service api around a combination of queries and choice
invocations. This is hopefully adequately demonstrated in the open API
definition. Other than that the usual web service engineering
considerations apply. Separation of concerns, DRY [31]_, and the
importance of centralising SQL generation and Authentication mechanisms
to ensure having to address these security sensitive components only
once.

Frontend Interface Structure
----------------------------

One property of the fully mediated architecture used in the example
application is that by delegating all operations to the backend, the
open API schemas act as DTO (Data Transfer Object) definitions for the
front and back ends [32]_. In simple cases, such as the example
application, these can double as front end models when using a React,
MVVM, FRP, or similar front end architecture style.

The example application is a naive React web frontend [33]_ written in
Typescript [34]_. It accesses the Backend web services using the
generator-less Axios client to handle the lowest level transport,
configured in `src/api.ts`:

.. code-block::

   import OpenAPIClientAxios from 'openapi-client-axios';
   import openApi from '../../common/openapi.yaml';

   const api = new OpenAPIClientAxios({
        definition: openApi as any,
        withServer: { url: '/api' },
   });

   api.init();

   export default api;

Authentication is handled using OAuth2 against a mock OAuth server [35]_
to perform the login; and, bearer tokens to identify the Frontend to the
Backend. The Frontend does not have any knowledge of Canton, or Daml
Users or Parties, this is delegated entirely to the Backend.

The records defined by the OpenAPI definition are used directly as the
models maintained within the react stores, and from there to the views
via the usual react handlers.

.. [19]
   https://daml.talentlms.com/catalog/info/id:152 currently part of the Daml Philosophy Certification
   https://daml.talentlms.com/catalog/info/id:149

.. [20]
   In particular the Solution Topology course https://daml.talentlms.com/catalog/info/id:161 within the larger TSA
   certification https://daml.talentlms.com/catalog/info/id:160

.. [21]
   Most make targets can be located by searching/grepping for ^target:.
   The main exceptions to this are the open-\* targets which are
   cross-platform and generated by macro at the end of the file.

.. [22]
   Found in docker/utils.sh

.. [23]
   https://docs.daml.com/tools/daml-shell/index.html#daml-shell-daml-shell

.. [24]
   https://docs.daml.com/daml/patterns/propose-accept.html

.. [25]
   `https://docs.sync.global/index.html <https://docs.dev.sync.global/index.html>`__

.. [26]
   https://docs.daml.com/query/pqs-user-guide.html#pqs

.. [27]
   https://www.openapis.org/

.. [28]
   As defined by Roy Fielding
   (https://ics.uci.edu/~fielding/pubs/dissertation/top.htm)

.. [29]
   Contract-ids and their underlying contract are nouns and can be
   represented as ReST resources. However, not only does this fail to
   capture the ongoing business entity that often outlives any single
   contract, it misses the fact that at the core of Daml are the
   authorized choices which are verbs and therefore do not play nicely
   with ReST assumptions.

.. [30]
   https://spring.io/projects/spring-boot

.. [31]
   Topic 9
   https://pragprog.com/titles/tpp20/the-pragmatic-programmer-20th-anniversary-edition/
   “Don’t Repeat Yourself”

.. [32]
   The CQRS alternative architecture does not use DTOs. Instead the
   backend services return Daml contracts directly. These are then
   generally deserialised directly into Javascript or Typescript
   objects, generated directly from the DAR files; and, used to populate
   the underlying frontend model. This direct coupling from Daml to
   Frontend can significantly simplify the code required for
   applications with requirements defined in terms of a Daml model. The
   mediated architecture is more suitable where the Frontend needs to
   incorporate sources of data additional to the Canton Ledger.

.. [33]
   https://react.dev/

.. [34]
   https://www.typescriptlang.org/

.. [35]
   This is being changed to use keycloak as the JST server.