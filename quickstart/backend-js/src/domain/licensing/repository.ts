import type pg from 'pg'
import { findActive, findActiveByContractId } from '../../pqs/contracts.js'

// Template IDs as used by PQS active() function (module:template, no package hash).
// These match Utils.getTemplateIdByClass(Clazz).qualifiedName() from the Java backend.
export const TEMPLATE_IDS = {
  AppInstall: 'Licensing.AppInstall:AppInstall',
  AppInstallRequest: 'Licensing.AppInstall:AppInstallRequest',
  License: 'Licensing.License:License',
  LicenseRenewalRequest: 'Licensing.License:LicenseRenewalRequest',
  AllocationRequest: 'Splice.Api.Token.AllocationRequestV1:AllocationRequest',
  Allocation: 'Splice.Api.Token.AllocationV1:Allocation'
} as const

export interface LicenseRow { contract_id: string; license_payload: Record<string, unknown> }
export interface LicenseWithRenewalRows {
  licenseContractId: string
  licensePayload: Record<string, unknown>
  renewals: Array<{ contractId: string; payload: Record<string, unknown>; allocationCid?: string }>
}

export const findActiveLicenses = async (pool: pg.Pool, party: string): Promise<LicenseWithRenewalRows[]> => {
  const sql = `
    SELECT license.contract_id    AS license_contract_id,
           license.payload        AS license_payload,
           renewal.contract_id    AS renewal_contract_id,
           renewal.payload        AS renewal_payload,
           allocation.contract_id AS allocation_contract_id
    FROM active($1) license
    LEFT JOIN active($2) renewal ON
        license.payload->>'licenseNum' = renewal.payload->>'licenseNum'
        AND license.payload->>'user' = renewal.payload->>'user'
    LEFT JOIN active($3) allocation ON
        renewal.payload->>'requestId' = allocation.payload->'allocation'->'settlement'->'settlementRef'->>'id'
        AND renewal.payload->>'user' = allocation.payload->'allocation'->'transferLeg'->>'sender'
    WHERE license.payload->>'user' = $4 OR license.payload->>'provider' = $4
    ORDER BY license.contract_id
  `
  const res = await pool.query(sql, [
    TEMPLATE_IDS.License,
    TEMPLATE_IDS.LicenseRenewalRequest,
    TEMPLATE_IDS.Allocation,
    party
  ])
  const map = new Map<string, LicenseWithRenewalRows>()
  for (const row of res.rows) {
    const licId: string = row.license_contract_id
    let entry = map.get(licId)
    if (entry === undefined) {
      entry = { licenseContractId: licId, licensePayload: row.license_payload, renewals: [] }
      map.set(licId, entry)
    }
    if (row.renewal_contract_id !== null) {
      entry.renewals.push({
        contractId: row.renewal_contract_id,
        payload: row.renewal_payload,
        allocationCid: row.allocation_contract_id ?? undefined
      })
    }
  }
  return [...map.values()]
}

export const findLicenseById = (pool: pg.Pool, contractId: string) =>
  findActiveByContractId(pool, TEMPLATE_IDS.License, contractId)

export const findActiveLicenseRenewalRequestById = (pool: pg.Pool, contractId: string) =>
  findActiveByContractId(pool, TEMPLATE_IDS.LicenseRenewalRequest, contractId)

export const findActiveAllocationRequestById = (pool: pg.Pool, contractId: string) =>
  findActiveByContractId(pool, TEMPLATE_IDS.AllocationRequest, contractId)

export const findAppInstallById = (pool: pg.Pool, contractId: string) =>
  findActiveByContractId(pool, TEMPLATE_IDS.AppInstall, contractId)

export const findAppInstallRequestById = (pool: pg.Pool, contractId: string) =>
  findActiveByContractId(pool, TEMPLATE_IDS.AppInstallRequest, contractId)

export const findActiveAppInstalls = (pool: pg.Pool) =>
  findActive(pool, TEMPLATE_IDS.AppInstall)

export const findActiveAppInstallRequests = (pool: pg.Pool) =>
  findActive(pool, TEMPLATE_IDS.AppInstallRequest)
