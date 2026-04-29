import type pg from 'pg'

export interface ActiveContract<P = Record<string, unknown>> {
  contractId: string
  payload: P
}

// active($1) is the PQS-provided PostgreSQL function returning active (non-archived) contracts.
// Column names confirmed from Pqs.java: contract_id, payload.

export const findActive = async <P = Record<string, unknown>>(
  pool: pg.Pool,
  templateId: string
): Promise<ActiveContract<P>[]> => {
  const res = await pool.query<{ contract_id: string; payload: P }>(
    'SELECT contract_id, payload FROM active($1)',
    [templateId]
  )
  return res.rows.map((r) => ({ contractId: r.contract_id, payload: r.payload }))
}

export const findActiveByContractId = async <P = Record<string, unknown>>(
  pool: pg.Pool,
  templateId: string,
  contractId: string
): Promise<ActiveContract<P> | undefined> => {
  const res = await pool.query<{ contract_id: string; payload: P }>(
    'SELECT contract_id, payload FROM active($1) WHERE contract_id = $2',
    [templateId, contractId]
  )
  const row = res.rows[0]
  if (row === undefined) return undefined
  return { contractId: row.contract_id, payload: row.payload }
}

