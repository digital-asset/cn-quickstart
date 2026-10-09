# node-happy-path

Smallest Node.js script that talks to Canton end-to-end. Targets
[#156][156] and [#59][59]: a developer who's never written against Canton
from Node should be able to read this and have a working mental model in
minutes — not hours.

[156]: https://github.com/digital-asset/cn-quickstart/issues/156
[59]: https://github.com/digital-asset/cn-quickstart/issues/59

`src/index.ts` is ~135 lines and shows, in order:

1. Mint a shared-secret JWT with the claim shape Canton's ledger API expects.
2. Build a `LedgerClient` from `@canton-network/core-ledger-client`.
3. Read active `AppInstall` contracts from the PQS Postgres replica.
4. Exercise `AppInstall_CreateLicense` via JSON Ledger API v2 — the
   choice argument is type-checked against the `dpm codegen-js` output,
   so a typo in `params`, `meta`, or `values` is a compile error.
5. Confirm the new `License` contract appears in PQS.

No DI, no Express, no OpenAPI router — by design. The point is to put
the moving parts on one screen.

## Prerequisites

- Node.js 22 LTS
- pnpm 9+ (`corepack enable && corepack prepare pnpm@9 --activate`)
- `dpm` on PATH (installed by `make build-daml` from `quickstart/`)

## Run it

The script needs the cn-quickstart stack running in **shared-secret**
auth mode. From `quickstart/`:

```bash
make setup     # choose AUTH_MODE=shared-secret when prompted
make build     # builds the Daml DARs and other artifacts
make start     # brings up the stack
```

Then complete one onboarding flow in the UI so an `AppInstall` contract
exists.

Build the example:

```bash
cd quickstart/examples/node-happy-path
pnpm codegen:daml         # dpm codegen-js → daml-bindings/
pnpm install              # picks up the workspace packages
```

Read the runtime values from the running stack:

```bash
# AppProvider party — populated by splice-onboarding inside the
# backend-service container.
APP_PROVIDER_PARTY_ID="$(docker compose exec -T backend-service \
  sh -c '. /onboarding/backend-service/on/app-provider.sh && \
         printf %s "$APP_PROVIDER_PARTY"')"

# Shared secret — whatever you configured when running `make setup`
# in shared-secret mode.
AUTH_SHARED_SECRET="<your-shared-secret-from-make-setup>"
```

Run:

```bash
LEDGER_API_URL=http://canton.localhost:3975 \
PQS_DATABASE_URL='postgresql://cnadmin:supersafe@localhost:5432/scribe' \
AUTH_SHARED_SECRET="$AUTH_SHARED_SECRET" \
APP_PROVIDER_PARTY_ID="$APP_PROVIDER_PARTY_ID" \
pnpm start
```

Expected output:

```
{"level":30,"msg":"minted shared-secret token"}
{"level":30,"baseUrl":"http://canton.localhost:3975","msg":"ledger client ready"}
{"level":30,"count":1,"msg":"active AppInstall contracts visible to provider"}
{"level":30,"contractId":"00...","msg":"exercising AppInstall_CreateLicense"}
{"level":30,"newLicenseCid":"00...","msg":"created License"}
{"level":30,"visibleInPqs":true,"msg":"PQS round-trip confirmation"}
{"level":30,"msg":"done"}
```

## Why a shared-secret JWT?

Self-signing keeps the example to one file. Real deployments use OIDC.

## What this intentionally glosses over

| Gloss | What a production caller would do |
|---|---|
| Bare PQS access via the `pg` driver | Wrap `active(template_id)` queries in a small repository layer with codegen-typed payloads |
| OAuth2 / multi-issuer JWT verification | Verify per-issuer JWKS, resolve party by `iss` claim, fall through to a tenant registry |
| Token Standard registry HTTP (renewals, allocations) | Use `@canton-network/core-token-standard` for choice contexts and registry calls |
| Cross-cut logging / OTel | Wire `@opentelemetry/sdk-node` before any `pg`/`http`/`express` import |
| Disclosed-contract forwarding on `exerciseChoice` | Pass `disclosedContracts` from the choice context onto `JsCommands` |

Each row is a real edge of the API a developer would otherwise hit by
trial and error. This script gets past line 1 so the rest is approachable.
