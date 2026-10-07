import type { LicenseWithRenewalRows } from './repository.js'

type P = Record<string, unknown>

const str = (p: P, k: string): string => p[k] as string
const num = (p: P, k: string): number => Number(p[k])
const meta = (p: P, k: string): { data: Record<string, string> } => {
  const m = p[k] as { values?: Record<string, string> }
  return { data: m?.values ?? {} }
}

export const mapAppInstallRequest = (contractId: string, payload: P) => ({
  contractId,
  provider: str(payload, 'provider'),
  user: str(payload, 'user'),
  meta: meta(payload, 'meta')
})

export const mapAppInstall = (contractId: string, payload: P) => ({
  contractId,
  provider: str(payload, 'provider'),
  user: str(payload, 'user'),
  meta: meta(payload, 'meta'),
  numLicensesCreated: num(payload, 'numLicensesCreated')
})

export const mapLicenseRenewalRequest = (contractId: string, payload: P, allocationCid?: string) => {
  // Daml Int round-trips as a string in the JSON Ledger API to preserve 64-bit precision,
  // so use BigInt and reduce before casting back to Number for the final "N days" string.
  const relTime = payload['licenseExtensionDuration'] as { microseconds: string } | undefined
  const micros = BigInt(relTime?.microseconds ?? '0')
  const approximateDays = Number(micros / (1_000_000n * 3600n * 24n)) + ' days'
  const now = Date.now()
  const prepareUntil = str(payload, 'prepareUntil')
  const settleBefore = str(payload, 'settleBefore')
  return {
    contractId,
    provider: str(payload, 'provider'),
    user: str(payload, 'user'),
    licenseNum: num(payload, 'licenseNum'),
    licenseFeeAmount: parseFloat(payload['licenseFeeAmount'] as string),
    licenseFeeInstrument: null,
    licenseExtensionDuration: approximateDays,
    prepareUntil,
    settleBefore,
    requestedAt: str(payload, 'requestedAt'),
    description: str(payload, 'description'),
    requestId: str(payload, 'requestId'),
    allocationCid: allocationCid ?? null,
    prepareDeadlinePassed: new Date(prepareUntil).getTime() <= now,
    settleDeadlinePassed: new Date(settleBefore).getTime() <= now
  }
}

export const mapLicense = (row: LicenseWithRenewalRows) => {
  const lp = row.licensePayload as P
  const now = Date.now()
  const expiresAt = str(lp, 'expiresAt')
  const params = lp['params'] as P
  return {
    contractId: row.licenseContractId,
    provider: str(lp, 'provider'),
    user: str(lp, 'user'),
    params: {
      meta: meta(params, 'meta')
    },
    expiresAt,
    licenseNum: num(lp, 'licenseNum'),
    isExpired: new Date(expiresAt).getTime() <= now,
    renewalRequests: row.renewals
      .map(r => mapLicenseRenewalRequest(r.contractId, r.payload as P, r.allocationCid))
      .sort((a, b) => new Date(a.requestedAt).getTime() - new Date(b.requestedAt).getTime())
  }
}
