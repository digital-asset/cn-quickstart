// Canton + Node.js minimal happy path.
//
// What this script demonstrates, end-to-end, in one file:
//   1. Mint a self-signed shared-secret JWT for the Canton ledger API.
//   2. Build a typed `LedgerClient` against Canton JSON Ledger API v2.
//   3. Read active `AppInstall` contracts from the PQS Postgres replica.
//   4. Exercise `AppInstall_CreateLicense` on the first one — argument
//      shape is type-checked against the `dpm codegen-js` output.
//   5. Confirm the new `License` lands in PQS.
//
// No DI, no Express, no OpenAPI router. The point is to put the moving
// parts on one screen.

import { LedgerClient } from '@canton-network/core-ledger-client'
import { AuthTokenProvider } from '@canton-network/core-wallet-auth'
import { Licensing } from '@daml.js/quickstart-licensing-0.0.1'
import { SignJWT } from 'jose'
import { Pool } from 'pg'
import pino from 'pino'

const log = pino({ level: process.env.LOG_LEVEL ?? 'info' })

function need(key: string): string {
  const v = process.env[key]
  if (v === undefined || v === '') {
    log.error({ key }, 'missing required env')
    process.exit(1)
  }
  return v
}

const LEDGER_API_URL = need('LEDGER_API_URL')
const PQS_DATABASE_URL = need('PQS_DATABASE_URL')
const AUTH_SHARED_SECRET = need('AUTH_SHARED_SECRET')
const PARTY_ID = need('APP_PROVIDER_PARTY_ID')

// Typed template references from `dpm codegen-js`. They expose a static
// `templateId` string (prefixed with `#package-name:` per @daml.js
// convention) plus typed Choice metadata.
//
// JSON Ledger API v2 accepts the `#`-prefixed form directly. PQS's
// `active(template_id)` SQL function requires the prefix stripped — that
// translation is the only ledger-vs-PQS gotcha you cannot infer from
// the OpenAPI types alone.
const { AppInstall } = Licensing.AppInstall
const { License } = Licensing.License
const pqsTid = (tid: string): string => tid.replace(/^#/, '')

// 1. Mint a shared-secret JWT with the claim shape Canton's ledger API
//    accepts in unsafe-secret mode. Production deployments use OIDC.
const secretBytes = new TextEncoder().encode(AUTH_SHARED_SECRET)
const token = await new SignJWT({
  aud: 'https://canton.network.global',
  sub: 'app-provider',
  scope: 'daml_ledger_api',
  party: PARTY_ID,
})
  .setProtectedHeader({ alg: 'HS256' })
  .setIssuedAt()
  .setExpirationTime('1h')
  .sign(secretBytes)
log.info('minted shared-secret token')

// 2. Build a typed LedgerClient. `postWithRetry` is generic over the
//    JSON v2 OpenAPI paths — typos in the path or body shape fail at
//    compile time. Works against Canton 3.4 + 3.5.
const ledger = new LedgerClient({
  baseUrl: new URL(LEDGER_API_URL),
  logger: log,
  accessTokenProvider: AuthTokenProvider.fromToken(token, log),
  version: '3.4',
})
await ledger.init()
log.info({ baseUrl: LEDGER_API_URL }, 'ledger client ready')

// 3. Read PQS. The `active(template_id)` SQL function returns the
//    currently-active contracts for the given template. PQS is a
//    Postgres materialized view of the ledger maintained by Scribe —
//    far cheaper to query for list views than the JSON ACS endpoint.
const pg = new Pool({ connectionString: PQS_DATABASE_URL })
const { rows: installs } = await pg.query<{
  contract_id: string
  payload: { provider: string; user: string }
}>("SELECT contract_id, payload FROM active($1) WHERE payload->>'provider' = $2", [
  pqsTid(AppInstall.templateId),
  PARTY_ID,
])
log.info({ count: installs.length }, 'active AppInstall contracts visible to provider')
if (installs.length === 0) {
  log.warn('no AppInstalls — start the stack and accept an AppUser onboarding flow first')
  await pg.end()
  process.exit(0)
}

// 4. Exercise AppInstall_CreateLicense. The argument is typed against
//    the codegen'd `AppInstall_CreateLicense` shape: `{ params:
//    LicenseParams }` where `LicenseParams.meta` is the splice
//    MetadataV1.Metadata `{ values: TextMap }`. A typo in `params`,
//    `meta`, or `values` is a compile error.
const target = installs[0]
if (target === undefined) throw new Error('unreachable')

const choiceArgument: Licensing.AppInstall.AppInstall_CreateLicense = {
  params: { meta: { values: {} } },
}

log.info({ contractId: target.contract_id }, 'exercising AppInstall_CreateLicense')
const result = await ledger.postWithRetry('/v2/commands/submit-and-wait-for-transaction', {
  commands: {
    commandId: `happy-path-${Date.now()}`,
    actAs: [PARTY_ID],
    commands: [
      {
        ExerciseCommand: {
          templateId: AppInstall.templateId,
          contractId: target.contract_id,
          choice: AppInstall.AppInstall_CreateLicense.choiceName,
          choiceArgument,
        },
      },
    ],
  },
  transactionFormat: {
    // `verbose` is documented as optional but Canton's runtime rejects
    // requests without it. Always include.
    eventFormat: { filtersByParty: { [PARTY_ID]: {} }, verbose: false },
    transactionShape: 'TRANSACTION_SHAPE_LEDGER_EFFECTS',
  },
})

const createdEvent = result.transaction.events.find((ev) => 'CreatedEvent' in ev)
const newLicenseCid =
  createdEvent !== undefined && 'CreatedEvent' in createdEvent
    ? createdEvent.CreatedEvent.contractId
    : undefined
log.info({ newLicenseCid }, 'created License')

// 5. Confirm via PQS. `active(License)` should now include the new
//    contract.
if (newLicenseCid !== undefined) {
  const { rows: licenses } = await pg.query<{ contract_id: string }>(
    'SELECT contract_id FROM active($1) WHERE contract_id = $2',
    [pqsTid(License.templateId), newLicenseCid],
  )
  log.info({ visibleInPqs: licenses.length === 1 }, 'PQS round-trip confirmation')
}

await pg.end()
log.info('done')
