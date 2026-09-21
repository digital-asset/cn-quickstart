import type { FastifyInstance } from 'fastify'
import type { BackendConfig } from '../config.js'
import type { TenantRepository } from '../tenants/repository.js'
import { resolveTestModePartyId } from './test-mode-party.js'

export const registerSharedSecret = async (app: FastifyInstance, cfg: BackendConfig, tenants: TenantRepository): Promise<void> => {
  if (cfg.authMode !== 'shared-secret') return

  app.post<{ Body: { username?: string } }>('/login', async (req, reply) => {
    const username = req.body?.username
    if (username === undefined || username === '') { reply.redirect('/login?error=missing_username'); return }
    const matchedTenant = tenants.list().find(t => t.users?.includes(username))
    if (matchedTenant === undefined) { reply.redirect('/login?error=unknown_user'); return }
    req.session.user = {
      name: username,
      tenantId: matchedTenant.tenantId,
      partyId: resolveTestModePartyId(cfg, req.session.user?.idTokenClaims, matchedTenant.partyId),
      userId: username,
      roles: matchedTenant.internal ? ['ROLE_ADMIN'] : ['ROLE_USER'],
      isAdmin: matchedTenant.internal
    }
    reply.redirect('/')
  })
}

export const registerLogout = async (app: FastifyInstance): Promise<void> => {
  app.post('/logout', async (req, reply) => {
    await req.session.destroy()
    reply.code(204).send()
  })
}
