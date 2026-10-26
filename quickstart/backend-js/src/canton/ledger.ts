import type { BackendConfig } from '../config.js'
import type { CantonTokenProvider } from './auth.js'

export interface DisclosedContract {
  contractId: string
  createdEventBlob: string
  synchronizerId: string
  templateId: string
}

export interface Command {
  CreateCommand?: { templateId: string; createArguments: unknown }
  ExerciseCommand?: { templateId: string; contractId: string; choice: string; choiceArgument: unknown }
}

export interface SubmitArgs {
  commandId: string
  actAs: string[]
  readAs?: string[]
  userId: string
  commands: Command[]
  disclosedContracts?: DisclosedContract[]
}

export class LedgerApi {
  constructor(private readonly cfg: BackendConfig, private readonly tokens: CantonTokenProvider) {}

  private async post<T>(path: string, body: unknown): Promise<T> {
    const token = await this.tokens.getToken()
    const headers: Record<string, string> = { 'content-type': 'application/json' }
    if (token !== undefined) headers['authorization'] = `Bearer ${token}`
    const res = await fetch(`${this.cfg.ledgerJsonApiBaseUrl}${path}`, { method: 'POST', headers, body: JSON.stringify(body) })
    if (!res.ok) throw new Error(`${path} ${res.status}: ${await res.text()}`)
    return await res.json() as T
  }

  async submitAndWaitForTransaction(args: SubmitArgs): Promise<unknown> {
    return this.post('/v2/commands/submit-and-wait-for-transaction', { commands: args })
  }

  async submitAndWaitForTransactionTree(args: SubmitArgs): Promise<unknown> {
    return this.post('/v2/commands/submit-and-wait-for-transaction-tree', { commands: args })
  }
}
