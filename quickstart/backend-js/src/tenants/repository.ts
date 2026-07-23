import type { BackendConfig } from '../config.js'

export interface Tenant {
  tenantId: string
  partyId: string
  walletUrl: string
  clientId: string
  issuerUrl: string
  internal: boolean
  users?: string[]
}

export class TenantRepository {
  private readonly tenants = new Map<string, Tenant>()

  list(): Tenant[] { return [...this.tenants.values()] }
  listExternal(): Tenant[] { return this.list().filter((t) => !t.internal) }
  get(tenantId: string): Tenant | undefined { return this.tenants.get(tenantId) }
  has(tenantId: string): boolean { return this.tenants.has(tenantId) }
  put(tenant: Tenant): void {
    if (this.tenants.has(tenant.tenantId)) throw new Error(`Duplicate tenantId: ${tenant.tenantId}`)
    this.tenants.set(tenant.tenantId, tenant)
  }
  upsert(tenant: Tenant): void { this.tenants.set(tenant.tenantId, tenant) }
  delete(tenantId: string): void {
    if (!this.tenants.delete(tenantId)) throw new Error(`No tenant: ${tenantId}`)
  }
}

export const seedAppProvider = (repo: TenantRepository, cfg: BackendConfig): void => {
  repo.put({
    tenantId: 'AppProvider',
    partyId: cfg.appProviderParty,
    walletUrl: '',
    clientId: cfg.oauth2?.backendOidcClientId ?? '',
    issuerUrl: cfg.oauth2?.issuerUrl ?? '',
    internal: true,
    // Mirrors Java's application-shared-secret.yml `application.tenants.AppProvider.users`.
    // Matches AUTH_APP_PROVIDER_WALLET_ADMIN_USER_NAME from docker/modules/localnet/env/app-provider-auth-on.env.
    users: cfg.authMode === 'shared-secret' ? ['app-provider'] : undefined
  })
}
