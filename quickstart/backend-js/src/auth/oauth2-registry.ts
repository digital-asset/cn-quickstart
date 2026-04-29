import * as openid from 'openid-client'

export interface RegistrationEntry {
  registrationId: string
  tenantId: string
  clientId: string
  issuerUrl: string
  config: openid.Configuration
}

export class OAuth2Registry {
  private readonly entries = new Map<string, RegistrationEntry>()

  async register(tenantId: string, clientId: string, issuerUrl: string): Promise<string> {
    const registrationId = tenantId === 'AppProvider' ? 'AppProvider' : `${tenantId}-${clientId}`
    if (this.entries.has(registrationId)) {
      throw new Error(`Registration already exists: ${registrationId}`)
    }
    // openid-client v6 rejects plain-HTTP issuers by default. Spring's default HTTP client
    // (used by the Java backend) doesn't enforce HTTPS, so opt into allowInsecureRequests for
    // http:// issuers to match. The execute hook propagates to all subsequent calls made
    // through the returned Configuration.
    //
    // allowInsecureRequests is annotated @deprecated by the library to make it stand out as a
    // safety override (not because it's going away or has a replacement); accepted here.
    const url = new URL(issuerUrl)
    const options: openid.DiscoveryRequestOptions | undefined =
      url.protocol === 'http:' ? { execute: [openid.allowInsecureRequests] } : undefined
    const config = await openid.discovery(url, clientId, undefined, undefined, options)
    this.entries.set(registrationId, { registrationId, tenantId, clientId, issuerUrl, config })
    return registrationId
  }

  get(registrationId: string): RegistrationEntry | undefined {
    return this.entries.get(registrationId)
  }

  list(): RegistrationEntry[] {
    return [...this.entries.values()]
  }

  removeByTenantId(tenantId: string): void {
    const keys = [...this.entries.entries()]
      .filter(([, e]) => e.tenantId === tenantId)
      .map(([k]) => k)
    if (keys.length === 0) throw new Error(`No registrations for tenant: ${tenantId}`)
    keys.forEach((k) => this.entries.delete(k))
  }

  loginUrl(registrationId: string): string {
    return `/oauth2/authorization/${registrationId}`
  }
}
