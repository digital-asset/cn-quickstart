..
  Copyright (c) 2026 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.

..
  SPDX-License-Identifier: Apache-2.0

.. _quickstart-external-parties-howto:

=====================================================
How to Onboard and Use External Parties in Quickstart
=====================================================

Introduction
------------

External parties control their cryptographic signing keys,
which removes the need to trust any participant node with transaction authorization.
External parties provide full control over transaction signing,
ensure regulatory compliance for transaction authorization,
and are independent of participant node operators.

Prerequisites
-------------

- Access to a participant node with admin API credentials
- OpenSSL or equivalent for key generation
- curl for API calls (or grpcurl for gRPC)

Quickstart LocalNet Setup
-------------------------

If you haven't installed the Canton Network Quickstart application, refer to :ref:`quickstart-cnqs-installation`.

.. code-block:: bash

  # Navigate to Quickstart directory and run setup
  cd cn-quickstart
  make setup
 
  # When prompted, select:
  # - Enable Observability? no
  # - Enable OAUTH2? no
  # - Party hint: use default
  # - Enable Test mode: no
 
  # Start LocalNet
  make start

Your ``.env.local`` file should look like:

.. code-block:: text

  OBSERVABILITY_ENABLED=false
  AUTH_MODE=shared-secret
  PARTY_HINT=quickstart-USERNAME-1
  TEST_MODE=off

**Obtain Admin Token**

The external party topology APIs require authentication.
In shared-secret mode, you generate a JWT token using the ``splice-onboarding`` container with ``app-user`` as the subject:

.. code-block:: bash

  ADMIN_TOKEN=$(docker exec splice-onboarding \
    jwt-cli encode hs256 --s unsafe --p '{"sub": "app-user", "aud": "https://canton.network.global"}')
 
  echo $ADMIN_TOKEN

Generate Cryptographic Keys
---------------------------

Create an Ed25519 key pair for the external party.

.. code-block:: bash

  # Generate Ed25519 private key in PEM format
  openssl genpkey -algorithm ed25519 -out external_party_private.pem

  # Extract raw public key (32 bytes) and convert to hex for API calls
  HEX_PUBLIC_KEY=$(openssl pkey -in external_party_private.pem -pubout -outform DER | tail -c 32 | xxd -p -c 32)
  echo "Hex public key: $HEX_PUBLIC_KEY"

Onboard an External Party
-------------------------

Generate Topology Transactions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Update the ``party_hint`` value below to match your ``.env.local`` configuration.

.. code-block:: bash

  # Extract party hint from .env.local
  PARTY_HINT=$(grep '^PARTY_HINT=' .env.local | cut -d= -f2)

Use the validator API to generate the three required topology transactions:

.. code-block:: bash

  GENERATE_RESPONSE=$(curl -sS -X POST http://localhost:2903/api/validator/v0/admin/external-party/topology/generate \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "party_hint": "'"$PARTY_HINT"'",
    "public_key": "'"$HEX_PUBLIC_KEY"'"
  }')

  echo "$GENERATE_RESPONSE" | jq .

**Example response:**

.. code-block:: json

  {
    "party_id": "quickstart-USERNAME-1::1220abc123...",
    "topology_txs": [
      {
        "topology_tx": "CowBCAEQAR...",
        "hash": "122032fd29c1..."
      },
      {
        "topology_tx": "Cr4BCAEQAb...",
        "hash": "122088b08d96..."
      },
      {
        "topology_tx": "CqIBCAEQAZ...",
        "hash": "12209ac948be..."
      }
    ]
  }

The response contains:

- ``party_id``: The allocated party identifier (party hint + key fingerprint)
- ``topology_txs``: Array of three topology transactions with their hashes:
  1. **Root namespace transaction** - Creates the party and sets the public key controlling the namespace
  2. **Party to participant mapping** - Hosts the party on the participant with Confirmation rights
  3. **Party to key mapping** - Sets the key to authorize Daml transactions

Sign Topology Transaction Hashes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Each topology transaction returned by the generate API has a ``hash`` field that must be signed with your private key.
The hash is hex-encoded.

**Extract the response values:**

The ``GENERATE_RESPONSE`` variable was set by the curl command above.
Now extract the party ID, topology transactions, and hashes:

.. code-block:: bash

  # Extract party_id for later use
  PARTY_ID=$(echo "$GENERATE_RESPONSE" | jq -r '.party_id')
  echo "Party ID: $PARTY_ID"
 
  # Extract key fingerprint from party_id (the part after ::)
  # This is needed for signing transactions in Part 3
  KEY_FINGERPRINT=${PARTY_ID##*::}
  echo "Key fingerprint: $KEY_FINGERPRINT"
 
  # Extract topology transactions and their hashes
  TOPOLOGY_TX_1=$(echo "$GENERATE_RESPONSE" | jq -r '.topology_txs[0].topology_tx')
  HASH_1=$(echo "$GENERATE_RESPONSE" | jq -r '.topology_txs[0].hash')
 
  TOPOLOGY_TX_2=$(echo "$GENERATE_RESPONSE" | jq -r '.topology_txs[1].topology_tx')
  HASH_2=$(echo "$GENERATE_RESPONSE" | jq -r '.topology_txs[1].hash')
 
  TOPOLOGY_TX_3=$(echo "$GENERATE_RESPONSE" | jq -r '.topology_txs[2].topology_tx')
  HASH_3=$(echo "$GENERATE_RESPONSE" | jq -r '.topology_txs[2].hash')
 
  echo "Hash 1: $HASH_1"
  echo "Hash 2: $HASH_2"
  echo "Hash 3: $HASH_3"

**Sign each hash with Ed25519 private key:**

The signing commands use temporary files for cross-platform compatibility:

.. code-block:: bash

  # Sign hash 1
  printf '%s' "$HASH_1" | xxd -r -p > /tmp/hash1.bin
  SIG_1=$(openssl pkeyutl -sign -inkey external_party_private.pem -rawin -in /tmp/hash1.bin | xxd -p | tr -d '\n')
  echo "Signature 1: $SIG_1"
 
  # Sign hash 2
  printf '%s' "$HASH_2" | xxd -r -p > /tmp/hash2.bin
  SIG_2=$(openssl pkeyutl -sign -inkey external_party_private.pem -rawin -in /tmp/hash2.bin | xxd -p | tr -d '\n')
  echo "Signature 2: $SIG_2"
 
  # Sign hash 3
  printf '%s' "$HASH_3" | xxd -r -p > /tmp/hash3.bin
  SIG_3=$(openssl pkeyutl -sign -inkey external_party_private.pem -rawin -in /tmp/hash3.bin | xxd -p | tr -d '\n')
  echo "Signature 3: $SIG_3"

.. note::

  The hashes and signatures are hex-encoded strings. The submit API expects:
 
  - ``topology_tx``: Base64-encoded topology transaction (as returned by generate)
  - ``signed_hash``: Hex-encoded Ed25519 signature of the transaction hash

Submit Signed Topology Transactions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: bash

  curl -X POST http://localhost:2903/api/validator/v0/admin/external-party/topology/submit \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
      "public_key": "'"$HEX_PUBLIC_KEY"'",
      "signed_topology_txs": [
        {
          "topology_tx": "'"$TOPOLOGY_TX_1"'",
          "signed_hash": "'"$SIG_1"'"
        },
        {
          "topology_tx": "'"$TOPOLOGY_TX_2"'",
          "signed_hash": "'"$SIG_2"'"
        },
        {
          "topology_tx": "'"$TOPOLOGY_TX_3"'",
          "signed_hash": "'"$SIG_3"'"
        }
      ]
    }'

**Successful response:**

.. code-block:: json

  {
    "party_id": "quickstart-USERNAME-1::1220abc123..."
  }

Validate Party Creation
^^^^^^^^^^^^^^^^^^^^^^^

After submitting the signed topology transactions, verify the external party was created successfully.

Generate a Ledger API token for the ``ledger-api-user``.

.. code-block:: bash

  LEDGER_TOKEN=$(docker exec splice-onboarding \
    jwt-cli encode hs256 --s unsafe --p '{"sub": "ledger-api-user", "aud": "https://canton.network.global"}')

.. code-block:: bash

  # Query parties endpoint to verify party exists
  curl -f -sS "http://localhost:2975/v2/parties?parties=$PARTY_ID" \
    -H "Authorization: Bearer $LEDGER_TOKEN"

**Discover Synchronizer ID**

The synchronizer ID identifies the network your participant is connected to and is required for topology validation and transaction submission.

.. code-block:: bash

  # Get the synchronizer ID from connected synchronizers
  SYNCHRONIZER_ID=$(curl -f -sS -L http://localhost:2975/v2/state/connected-synchronizers \
    -H "Authorization: Bearer $LEDGER_TOKEN" | jq -r ".connectedSynchronizers[0].synchronizerId")
 
  echo "Synchronizer ID: $SYNCHRONIZER_ID"

This typically returns a value like ``global-domain::12209d604bfb...``.

.. code-block:: bash

  # List party-to-participant mappings to verify party is in topology
  grpcurl -plaintext -d '{
    "base_query": {
      "store": {
        "synchronizer": {"id": "'"$SYNCHRONIZER_ID"'"}
      },
      "head_state": {}
    },
    "filter_party": "'"$PARTY_ID"'"
  }' localhost:2902 com.digitalasset.canton.topology.admin.v30.TopologyManagerReadService/ListPartyToParticipant

.. note::

  Topology transaction submission is asynchronous.
  The party may take a few seconds to appear in the topology state after successful submission.
  Implement a retry loop with a short delay if immediate verification is required.

Submit Transactions as the External Party
-----------------------------------------

Overview
^^^^^^^^

Unlike internal parties (1-step submission),
external parties use a 3-step interactive submission process:

1. **Prepare** - Request transaction preparation from a participant node
2. **Sign** - Sign the transaction hash with external key
3. **Execute** - Submit the signed transaction

Step 1: Prepare the Transaction
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Use the ``InteractiveSubmissionService/PrepareSubmission`` gRPC endpoint to prepare your transaction.

The ``Canton.Internal.Ping`` template is available on all Canton participants without deploying any DAR files.
The Ping template requires an ``initiator`` (your external party) and a ``responder`` (any other known party).

Retrieve the ``app_user`` party from your Quickstart LocalNet to use as responder:

.. code-block:: bash

  RESPONDER_PARTY=$(grpcurl -plaintext -H "Authorization: Bearer $LEDGER_TOKEN" localhost:2901 \
    com.daml.ledger.api.v2.admin.PartyManagementService/ListKnownParties | \
    jq -r '.party_details[] | select(.party | startswith("app_user")) | .party' | head -1)

  echo "Responder party: $RESPONDER_PARTY"

Submit the ``Ping`` contract:

.. code-block:: bash

  grpcurl -emit-defaults -plaintext \
    -H "Authorization: Bearer $LEDGER_TOKEN" \
    -d '{
      "user_id": "ledger-api-user",
      "command_id": "'"$(uuidgen)"'",
      "act_as": ["'"$PARTY_ID"'"],
      "synchronizer_id": "'"$SYNCHRONIZER_ID"'",
      "commands": [
        {
          "create": {
            "template_id": {
              "package_id": "#canton-builtin-admin-workflow-ping",
              "module_name": "Canton.Internal.Ping",
              "entity_name": "Ping"
            },
            "create_arguments": {
              "fields": [
                { "label": "id", "value": { "text": "external-party-ping-test" } },
                { "label": "initiator", "value": { "party": "'"$PARTY_ID"'" } },
                { "label": "responder", "value": { "party": "'"$RESPONDER_PARTY"'" } }
              ]
            }
          }
        }
      ]
    }' localhost:2901 \
    com.daml.ledger.api.v2.interactive.InteractiveSubmissionService/PrepareSubmission \
    > prepare_response.json

.. note::

  The ``user_id`` must be ``ledger-api-user`` to match the JWT subject used for the ``LEDGER_TOKEN``.

.. tip::

  To use your own Daml templates instead of the built-in Ping, replace the ``template_id`` fields with your package ID,
  module name, and template name. Discover deployed packages using:
 
  .. code-block:: bash
 
     grpcurl -plaintext -H "Authorization: Bearer $LEDGER_TOKEN" localhost:2901 \
       com.daml.ledger.api.v2.PackageService/ListPackages

**Response fields:**

- ``prepared_transaction``: The full transaction and metadata to be signed
- ``prepared_transaction_hash``: Pre-computed hash (recompute client-side for security)
- ``hashing_scheme_version``: Version of the hashing algorithm (typically ``HASHING_SCHEME_VERSION_V2``)

Extract the prepared transaction and hash for signing:

.. code-block:: bash

  PREPARED_TRANSACTION=$(cat prepare_response.json | jq .prepared_transaction)
  TRANSACTION_HASH=$(cat prepare_response.json | jq -r .prepared_transaction_hash)

Step 2: Validate and Sign
^^^^^^^^^^^^^^^^^^^^^^^^^

**1. Validate the Transaction**

Before signing, inspect the prepared transaction to verify it matches your intent:

.. code-block:: bash

  cat prepare_response.json | jq .prepared_transaction

**2. Sign the Hash**

Sign the ``$TRANSACTION_HASH`` returned by ``PrepareSubmission`` with your Ed25519 private key:

.. note::

  For production deployments, you may want to recompute the transaction hash client-side rather than trusting the pre-computed hash.
  A Python implementation is available in the Canton release artifact at
  ``examples/08-interactive-submission/daml_transaction_hashing_v2.py``.

**Sign the hash with your private key:**

.. code-block:: bash

  # Decode base64 hash to temp file
  printf '%s' "$TRANSACTION_HASH" | base64 --decode > /tmp/tx_hash.bin
 
  # Sign and encode to base64
  SIGNATURE=$(openssl pkeyutl -sign -inkey external_party_private.pem -rawin -in /tmp/tx_hash.bin | base64 | tr -d '\n')

Store the signature for the execute step.

Step 3: Execute Submission
^^^^^^^^^^^^^^^^^^^^^^^^^^

Submit the signed transaction using ``InteractiveSubmissionService/ExecuteSubmission``:

.. code-block:: bash

  SUBMISSION_ID=$(uuidgen)
 
  grpcurl -emit-defaults -plaintext \
    -H "Authorization: Bearer $LEDGER_TOKEN" \
    -d '{
      "prepared_transaction": '"$PREPARED_TRANSACTION"',
      "hashing_scheme_version": "HASHING_SCHEME_VERSION_V2",
      "user_id": "ledger-api-user",
      "submission_id": "'"$SUBMISSION_ID"'",
      "party_signatures": {
        "signatures": [
          {
            "party": "'"$PARTY_ID"'",
            "signatures": [
              {
                "format": "SIGNATURE_FORMAT_CONCAT",
                "signature": "'"$SIGNATURE"'",
                "signing_algorithm_spec": "SIGNING_ALGORITHM_SPEC_ED25519",
                "signed_by": "'"$KEY_FINGERPRINT"'"
              }
            ]
          }
        ]
      }
    }' localhost:2901 \
    com.daml.ledger.api.v2.interactive.InteractiveSubmissionService/ExecuteSubmission

**Key fields:**

- ``submission_id``: A new UUID for this submission attempt (can retry with a new ID without re-signing)
- ``party_signatures``: Contains the signature with format, algorithm spec, and the signing key fingerprint

Observe Transaction Outcome
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Verify the transaction was processed using the ``CompletionStream`` endpoint.

.. note::

  ``CompletionStream`` is a blocking endpoint that waits for new completions.
  Open a **second terminal** and run this command **before** executing Step 3,
  or re-generate ``LEDGER_TOKEN`` and ``PARTY_ID`` in the new terminal first.

In your second terminal:

.. code-block:: bash

  grpcurl -emit-defaults -plaintext -H "Authorization: Bearer $LEDGER_TOKEN" -d '{
    "user_id": "ledger-api-user",
    "parties": ["'"$PARTY_ID"'"]
  }' localhost:2901 \
    com.daml.ledger.api.v2.CommandCompletionService/CompletionStream \
    > completion_response.json

The stream captures the completion after executing Step 3 in your original terminal.
Stop the stream, then inspect the result:

.. code-block:: bash

  cat completion_response.json | jq .

  A ``status.code`` of ``0`` indicates success:

.. code-block:: json

  {
    "completion": {
      "command_id": "your-command-id",
      "status": { "code": 0, "message": "" },
      "update_id": "1220...",
      "offset": "24"
    }
  }

**Query Transaction Details (Optional)**

Extract the ``offset`` from the completion and use ``GetUpdates`` to retrieve full transaction details:

.. code-block:: bash

  COMPLETION_OFFSET=$(cat completion_response.json | jq -r '.completion.offset')

  grpcurl -emit-defaults -plaintext -H "Authorization: Bearer $LEDGER_TOKEN" -d '{
    "begin_exclusive": '"$((COMPLETION_OFFSET - 1))"',
    "end_inclusive": '"$COMPLETION_OFFSET"',
    "update_format": {
      "include_transactions": {
        "transaction_shape": "TRANSACTION_SHAPE_ACS_DELTA",
        "event_format": {
          "filters_by_party": {
            "'"$PARTY_ID"'": {
              "cumulative": [{ "wildcard_filter": {} }]
            }
          }
        }
      }
    }
  }' localhost:2901 \
    com.daml.ledger.api.v2.UpdateService/GetUpdates

.. note::

  External parties authenticate via cryptographic signatures rather than ledger user rights.
  This means ``GetUpdateById`` (which requires ``can_read_as`` rights) won't work for external party transactions.
  Use ``GetUpdates`` with the offset range instead.
