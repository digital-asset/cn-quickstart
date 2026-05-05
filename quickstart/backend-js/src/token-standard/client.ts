import type { BackendConfig } from '../config.js'

export class TokenStandardClient {
  constructor(private readonly cfg: BackendConfig) {}

  private async get<T>(path: string): Promise<T> {
    const res = await fetch(`${this.cfg.registryBaseUri}${path}`)
    if (!res.ok) throw new Error(`${path} ${res.status}: ${await res.text()}`)
    return await res.json() as T
  }

  private async post<T>(path: string, body: unknown): Promise<T> {
    const res = await fetch(`${this.cfg.registryBaseUri}${path}`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body)
    })
    if (!res.ok) throw new Error(`${path} ${res.status}: ${await res.text()}`)
    return await res.json() as T
  }

  async getRegistryAdminId(): Promise<{ adminId: string }> {
    return this.get<{ adminId: string }>('/registry/metadata/v1/info')
  }

  async getAllocationTransferContext(allocationCid: string): Promise<unknown> {
    return this.post(
      `/registry/allocations/v1/${encodeURIComponent(allocationCid)}/choice-contexts/execute-transfer`,
      {}
    )
  }
}
