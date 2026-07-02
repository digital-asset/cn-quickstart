import { randomUUID } from 'node:crypto'
import type pg from 'pg'
import type * as damlTypes from '@daml/types'
import { AppInstall, AppInstallRequest } from '@daml.js/quickstart-licensing-0.0.1/lib/Licensing/AppInstall/module.js'
import { License, LicenseRenewalRequest } from '@daml.js/quickstart-licensing-0.0.1/lib/Licensing/License/module.js'
import { AllocationRequest } from '@daml.js/splice-api-token-allocation-request-v1-1.0.0/lib/Splice/Api/Token/AllocationRequestV1/module.js'
import type { Allocation } from '@daml.js/splice-api-token-allocation-v1-1.0.0/lib/Splice/Api/Token/AllocationV1/module.js'
import type { AnyContract, AnyValue } from '@daml.js/splice-api-token-metadata-v1-1.0.0/lib/Splice/Api/Token/MetadataV1/module.js'
import type { BackendConfig } from '../../config.js'
import type { LedgerApi } from '../../canton/ledger.js'
import { exerciseChoice, submitContextFromSession } from '../../canton/commands.js'
import type { TokenStandardClient } from '../../token-standard/client.js'
import type { TenantRepository } from '../../tenants/repository.js'
import {
  findActiveLicenses,
  findLicenseById,
  findActiveLicenseRenewalRequestById,
  findActiveAllocationRequestById,
  findAppInstallById,
  findAppInstallRequestById,
  findActiveAppInstalls,
  findActiveAppInstallRequests
} from './repository.js'
import { mapAppInstallRequest, mapAppInstall, mapLicense } from './mappers.js'

const cidOf = <T>(s: string): damlTypes.ContractId<T> => s as damlTypes.ContractId<T>

export class LicensingService {
  constructor(
    private readonly cfg: BackendConfig,
    private readonly pool: pg.Pool,
    private readonly ledger: LedgerApi,
    private readonly tokenStandard: TokenStandardClient,
    private readonly tenants: TenantRepository
  ) {}

  async listAppInstallRequests(sessionParty: string) {
    const contracts = await findActiveAppInstallRequests(this.pool)
    return contracts
      .filter(c => {
        const p = c.payload as Record<string, unknown>
        return p['user'] === sessionParty || p['provider'] === sessionParty
      })
      .map(c => mapAppInstallRequest(c.contractId, c.payload as Record<string, unknown>))
  }

  async acceptAppInstallRequest(contractId: string, commandId: string | undefined, body: { installMeta?: { data?: Record<string, string> }; meta?: { data?: Record<string, string> } }) {
    const contract = await findAppInstallRequestById(this.pool, contractId)
    if (contract === undefined) return { status: 404 as const, message: `AppInstallRequest not found for contract ${contractId}` }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    await exerciseChoice(
      this.ledger, ctx,
      AppInstallRequest, AppInstallRequest.AppInstallRequest_Accept,
      contractId,
      {
        installMeta: { values: body.installMeta?.data ?? {} },
        meta: { values: body.meta?.data ?? {} }
      }
    )
    const payload = contract.payload as Record<string, unknown>
    return {
      status: 201 as const,
      body: {
        provider: payload['provider'] as string,
        user: payload['user'] as string,
        meta: { data: body.installMeta?.data ?? {} },
        numLicensesCreated: 0
      }
    }
  }

  async rejectAppInstallRequest(contractId: string, commandId: string | undefined, body: { meta?: { data?: Record<string, string> } }) {
    const contract = await findAppInstallRequestById(this.pool, contractId)
    if (contract === undefined) return { status: 404 as const, message: `AppInstallRequest not found for contract ${contractId}` }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    await exerciseChoice(
      this.ledger, ctx,
      AppInstallRequest, AppInstallRequest.AppInstallRequest_Reject,
      contractId,
      { meta: { values: body.meta?.data ?? {} } }
    )
    return { status: 204 as const }
  }

  async listAppInstalls(sessionParty: string) {
    const contracts = await findActiveAppInstalls(this.pool)
    return contracts
      .filter(c => {
        const p = c.payload as Record<string, unknown>
        return p['provider'] === sessionParty || p['user'] === sessionParty
      })
      .map(c => mapAppInstall(c.contractId, c.payload as Record<string, unknown>))
  }

  async createLicense(contractId: string, commandId: string | undefined, body: { params?: { meta?: { data?: Record<string, string> } } }) {
    const contract = await findAppInstallById(this.pool, contractId)
    if (contract === undefined) return { status: 404 as const, message: `AppInstall not found for contract ${contractId}` }
    const payload = contract.payload as Record<string, unknown>
    if (payload['provider'] !== this.cfg.appProviderParty) {
      return { status: 403 as const, message: 'Insufficient permissions' }
    }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    const result = await exerciseChoice(
      this.ledger, ctx,
      AppInstall, AppInstall.AppInstall_CreateLicense,
      contractId,
      { params: { meta: { values: body.params?.meta?.data ?? {} } } }
    )
    const licenseId = findCreatedContract(result, ':Licensing.License:License')
    if (licenseId === null) return { status: 500 as const, message: 'Failed to locate created License in transaction events' }
    return {
      status: 201 as const,
      body: { installId: contractId, licenseId }
    }
  }

  async cancelAppInstall(contractId: string, commandId: string | undefined, sessionParty: string, body: { meta?: { data?: Record<string, string> } }) {
    const contract = await findAppInstallById(this.pool, contractId)
    if (contract === undefined) return { status: 404 as const, message: `AppInstall not found for contract ${contractId}` }
    const payload = contract.payload as Record<string, unknown>
    if (sessionParty !== payload['user'] && sessionParty !== payload['provider']) {
      return { status: 403 as const, message: `party ${sessionParty} is not the user nor provider` }
    }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    await exerciseChoice(
      this.ledger, ctx,
      AppInstall, AppInstall.AppInstall_Cancel,
      contractId,
      {
        actor: this.cfg.appProviderParty,
        meta: { values: body.meta?.data ?? {} }
      }
    )
    return { status: 204 as const }
  }

  async listLicenses(sessionParty: string) {
    const rows = await findActiveLicenses(this.pool, sessionParty)
    return rows
      .map(mapLicense)
      .sort((a, b) => {
        const u = a.user.localeCompare(b.user)
        return u !== 0 ? u : a.licenseNum - b.licenseNum
      })
  }

  async renewLicense(contractId: string, commandId: string | undefined, body: {
    licenseFeeCc: number
    licenseExtensionDuration: string
    prepareUntilDuration: string
    settleBeforeDuration: string
    description: string
  }) {
    const [adminId, license] = await Promise.all([
      this.tokenStandard.getRegistryAdminId(),
      findLicenseById(this.pool, contractId)
    ])
    if (license === undefined) return { status: 404 as const, message: `License not found for contract ${contractId}` }
    const nowMs = Date.now()
    const durationToMicros = (iso: string): string => {
      const ms = parseDurationMs(iso)
      return String(ms * 1000)
    }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    await exerciseChoice(
      this.ledger, ctx,
      License, License.License_Renew,
      contractId,
      {
        requestId: randomUUID(),
        licenseFeeInstrumentId: { admin: adminId.adminId, id: 'Amulet' },
        licenseFeeAmount: String(body.licenseFeeCc),
        licenseExtensionDuration: { microseconds: durationToMicros(body.licenseExtensionDuration) },
        requestedAt: toIsoMicros(nowMs),
        prepareUntil: toIsoMicros(nowMs + parseDurationMs(body.prepareUntilDuration)),
        settleBefore: toIsoMicros(nowMs + parseDurationMs(body.settleBeforeDuration)),
        description: body.description
      }
    )
    return { status: 201 as const }
  }

  async completeLicenseRenewal(contractId: string, commandId: string | undefined, body: {
    renewalRequestContractId: string
    allocationContractId: string
  }) {
    const [choiceCtx, renewal] = await Promise.all([
      this.tokenStandard.getAllocationTransferContext(body.allocationContractId),
      findActiveLicenseRenewalRequestById(this.pool, body.renewalRequestContractId)
    ])
    if (choiceCtx === null || choiceCtx === undefined) {
      return { status: 404 as const, message: `Transfer context not found for allocation ${body.allocationContractId}` }
    }
    if (renewal === undefined) {
      return { status: 404 as const, message: `Active renewal request not found for contract ${body.renewalRequestContractId}` }
    }
    const ctx = (choiceCtx as { disclosedContracts?: Array<{ templateId: string; contractId: string; createdEventBlob: string; synchronizerId: string }> })
    const disclosed = ctx.disclosedContracts ?? []

    const metaMap: Record<string, string> = {
      AmuletRules: 'amulet-rules',
      OpenMiningRound: 'open-round'
    }
    const extraArgsValues: Record<string, AnyValue> = {}
    for (const dc of disclosed) {
      const parts = dc.templateId.split(':')
      const entityName = parts[parts.length - 1] ?? ''
      const key = metaMap[entityName]
      if (key !== undefined) {
        extraArgsValues[key] = { tag: 'AV_ContractId', value: cidOf<AnyContract>(dc.contractId) }
      }
    }

    const disclosedForLedger = disclosed.map(dc => ({
      contractId: dc.contractId,
      createdEventBlob: dc.createdEventBlob,
      synchronizerId: dc.synchronizerId,
      templateId: dc.templateId
    }))

    const submitCtx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    const result = await exerciseChoice(
      this.ledger, submitCtx,
      LicenseRenewalRequest, LicenseRenewalRequest.LicenseRenewalRequest_CompleteRenewal,
      body.renewalRequestContractId,
      {
        allocationCid: cidOf<Allocation>(body.allocationContractId),
        licenseCid: cidOf<License>(contractId),
        extraArgs: {
          context: { values: extraArgsValues },
          meta: { values: {} }
        }
      },
      disclosedForLedger
    )
    const newLicenseId = findCreatedContract(result, ':Licensing.License:License')
    return { status: 200 as const, body: { licenseId: newLicenseId ?? undefined } }
  }

  async expireLicense(contractId: string, commandId: string | undefined, sessionParty: string, body: { meta?: { data?: Record<string, string> } }) {
    const license = await findLicenseById(this.pool, contractId)
    if (license === undefined) return { status: 404 as const, message: `License not found for contract ${contractId}` }
    const metaData: Record<string, string> = { ...(body.meta?.data ?? {}) }
    if (sessionParty !== this.cfg.appProviderParty) {
      metaData['Note'] = 'Triggered by user request'
    }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    await exerciseChoice(
      this.ledger, ctx,
      License, License.License_Expire,
      contractId,
      {
        actor: this.cfg.appProviderParty,
        meta: { values: metaData }
      }
    )
    return { status: 200 as const, body: 'License expired successfully' }
  }

  async withdrawLicenseRenewalRequest(contractId: string, commandId: string | undefined) {
    const allocationReq = await findActiveAllocationRequestById(this.pool, contractId)
    if (allocationReq === undefined) return { status: 404 as const, message: `AllocationRequest ${contractId} not found` }
    const ctx = submitContextFromSession(this.cfg, this.cfg.appProviderParty, commandId)
    await exerciseChoice(
      this.ledger, ctx,
      AllocationRequest, AllocationRequest.AllocationRequest_Withdraw,
      contractId,
      { extraArgs: { context: { values: {} }, meta: { values: {} } } }
    )
    return { status: 204 as const }
  }
}

// `submit-and-wait-for-transaction` returns events in ACS_DELTA shape (CreatedEvent /
// ArchivedEvent only) and does NOT include the choice's exerciseResult. To find a contract
// the choice just created, walk the transaction's CreatedEvents and match on a templateId
// suffix (e.g. ':Licensing.License:License').
type CantonTxResponse = {
  transaction?: {
    events?: Array<{ CreatedEvent?: { templateId?: string; contractId?: string } }>
  }
}
const findCreatedContract = (response: unknown, templateIdSuffix: string): string | null => {
  const events = (response as CantonTxResponse).transaction?.events ?? []
  for (const event of events) {
    const ce = event.CreatedEvent
    if (ce === undefined) continue
    if (typeof ce.templateId !== 'string' || !ce.templateId.endsWith(templateIdSuffix)) continue
    if (typeof ce.contractId !== 'string') continue
    return ce.contractId
  }
  return null
}

// Daml `Time` is microsecond-precision. Date.toISOString() emits only milliseconds, which
// round-trips through Canton as `xxx000 µs` and fails to match the wallet's allocation
// settlement on `LicenseRenewalRequest_CompleteRenewal` (exact-equality comparison).
// Pad with sub-millisecond noise from process.hrtime to emit 6-decimal precision; the wallet
// copies the timestamp verbatim, so only the format width matters — the lower 3 digits don't
// need to track wall-clock microseconds.
const toIsoMicros = (ms: number): string => {
  const subMsNs = Number(process.hrtime.bigint() % 1_000_000n)
  const us = Math.floor(subMsNs / 1000)
  const iso = new Date(ms).toISOString()
  return iso.replace(/\.(\d{3})Z$/, `.$1${String(us).padStart(3, '0')}Z`)
}

// Mirrors java.time.Duration.parse semantics: only days, hours, minutes, seconds are
// supported. Months and years are rejected because their length is not fixed.
const parseDurationMs = (iso: string): number => {
  const m = iso.match(/^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?)?$/)
  if (m === null) throw new Error(`Invalid ISO-8601 duration (only days/hours/minutes/seconds supported): ${iso}`)
  const days = Number(m[1] ?? 0)
  const hours = Number(m[2] ?? 0)
  const minutes = Number(m[3] ?? 0)
  const seconds = Number(m[4] ?? 0)
  return (
    days * 24 * 3600_000 +
    hours * 3600_000 +
    minutes * 60_000 +
    Math.round(seconds * 1000)
  )
}
