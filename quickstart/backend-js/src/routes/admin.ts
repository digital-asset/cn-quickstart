import type { FastifyInstance } from 'fastify'
import type { BackendConfig } from '../config.js'
import type { TenantRepository } from '../tenants/repository.js'
import type { OAuth2Registry } from '../auth/oauth2-registry.js'
import { checkAdmin } from '../auth/admin-gate.js'

interface TenantRegistrationRequest {
  tenantId?: string
  partyId?: string
  walletUrl?: string
  clientId?: string
  issuerUrl?: string
  internal?: boolean
  users?: string[]
}

export const registerAdmin = async (app: FastifyInstance, cfg: BackendConfig, repo: TenantRepository, oauth2Registry: OAuth2Registry): Promise<void> => {

  app.get('/admin/tenant-registrations', async (req, reply) => {
    if (!await checkAdmin(cfg, req, reply)) return

    if (cfg.authMode === 'oauth2') {
      return oauth2Registry.list().map((entry) => {
        const tenant = repo.get(entry.tenantId)
        return {
          tenantId: entry.tenantId,
          partyId: tenant?.partyId ?? '',
          walletUrl: tenant?.walletUrl ?? '',
          clientId: entry.clientId,
          issuerUrl: entry.issuerUrl,
          internal: tenant?.internal ?? false
        }
      })
    }
    return repo.list().map((t) => ({
      tenantId: t.tenantId,
      partyId: t.partyId,
      walletUrl: t.walletUrl ?? '',
      internal: t.internal,
      users: t.users
    }))
  })

  app.post<{ Body: TenantRegistrationRequest }>('/admin/tenant-registrations', async (req, reply) => {
    if (!await checkAdmin(cfg, req, reply)) return

    const body = req.body
    if (!body.tenantId || !body.partyId) {
      reply.code(400); return { message: 'tenantId and partyId are required' }
    }
    if (cfg.authMode === 'oauth2') {
      if (!body.clientId || !body.issuerUrl) {
        reply.code(400); return { message: 'clientId and issuerUrl are required in OAuth2 mode' }
      }
      const duplicate = oauth2Registry.list().some(
        (e) => e.clientId === body.clientId && e.issuerUrl === body.issuerUrl
      )
      if (duplicate) {
        reply.code(409); return { message: 'ClientId-IssuerUrl combination already exists' }
      }
    } else {
      if (!body.users || body.users.length === 0) {
        reply.code(400); return { message: 'At least one user is required in shared-secret mode' }
      }
    }
    if (repo.has(body.tenantId)) {
      reply.code(409); return { message: 'TenantId already exists' }
    }

    if (cfg.authMode === 'oauth2' && body.clientId && body.issuerUrl) {
      await oauth2Registry.register(body.tenantId, body.clientId, body.issuerUrl)
    }
    repo.upsert({
      tenantId: body.tenantId,
      partyId: body.partyId,
      walletUrl: body.walletUrl ?? '',
      clientId: body.clientId ?? '',
      issuerUrl: body.issuerUrl ?? '',
      internal: body.internal ?? false,
      users: body.users
    })
    reply.code(201)
    return {
      tenantId: body.tenantId,
      partyId: body.partyId,
      walletUrl: body.walletUrl ?? '',
      clientId: body.clientId,
      issuerUrl: body.issuerUrl,
      internal: body.internal ?? false,
      users: body.users
    }
  })

  app.delete<{ Params: { tenantId: string } }>('/admin/tenant-registrations/:tenantId', async (req, reply) => {
    if (!await checkAdmin(cfg, req, reply)) return

    if (!repo.has(req.params.tenantId)) {
      reply.code(404); return { message: 'Not found' }
    }
    try {
      if (cfg.authMode === 'oauth2') {
        oauth2Registry.removeByTenantId(req.params.tenantId)
      }
      repo.delete(req.params.tenantId)
    } catch (err) {
      reply.code(500); return { message: (err as Error).message }
    }
    reply.code(204).send()
  })
}
