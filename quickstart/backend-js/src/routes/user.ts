import type { FastifyInstance } from 'fastify'
import type { TenantRepository } from '../tenants/repository.js'

export const registerUser = async (app: FastifyInstance, tenants: TenantRepository): Promise<void> => {
  app.get('/user', async (req, reply) => {
    const u = req.session.user
    if (u === undefined) { reply.code(401).send({ message: 'unauthorized' }); return }
    const tenant = tenants.get(u.tenantId)
    const party = u.partyId !== '' ? u.partyId : tenant?.partyId ?? ''
    const walletUrl = tenant?.walletUrl ?? ''
    const roles = u.roles ?? (u.isAdmin === true ? ['ROLE_ADMIN'] : ['ROLE_USER'])
    const isAdmin = u.isAdmin ?? roles.includes('ROLE_ADMIN')

    return { name: u.name, party, roles, isAdmin, walletUrl }
  })
}
