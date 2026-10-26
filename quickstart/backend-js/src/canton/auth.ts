import type { BackendConfig } from '../config.js'

interface CachedToken { token: string; expiresAt: number }

export class CantonTokenProvider {
  private cached?: CachedToken

  constructor(private readonly cfg: BackendConfig) {}

  async getToken(): Promise<string | undefined> {
    if (this.cfg.authMode === 'shared-secret') return this.cfg.sharedSecretToken
    if (this.cfg.authMode !== 'oauth2' || this.cfg.oauth2 === undefined) return undefined
    const now = Date.now()
    if (this.cached !== undefined && this.cached.expiresAt > now + 30_000) return this.cached.token

    const tokenUrl = `${this.cfg.oauth2.issuerUrl.replace(/\/$/, '')}/protocol/openid-connect/token`
    const body = new URLSearchParams({
      grant_type: 'client_credentials',
      client_id: this.cfg.oauth2.backendClientId,
      client_secret: this.cfg.oauth2.backendClientSecret
    })
    const res = await fetch(tokenUrl, { method: 'POST', body, headers: { 'content-type': 'application/x-www-form-urlencoded' } })
    if (!res.ok) throw new Error(`token endpoint ${res.status}: ${await res.text()}`)
    const json = await res.json() as { access_token: string; expires_in: number }
    this.cached = { token: json.access_token, expiresAt: now + json.expires_in * 1000 }
    return json.access_token
  }
}
