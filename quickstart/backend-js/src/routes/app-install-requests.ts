import type { FastifyInstance } from 'fastify'
import type { Services } from '../http/app.js'
import { checkAdmin } from '../auth/admin-gate.js'

export const registerAppInstallRequests = async (app: FastifyInstance, services: Services): Promise<void> => {
  const { cfg, licensing } = services

  app.get('/app-install-requests', async (req, reply) => {
    if (req.session.user === undefined) { reply.code(401).send({ message: 'unauthorized' }); return }
    return licensing.listAppInstallRequests(req.session.user.partyId)
  })

  // find-my-way (Fastify v5's router) does not support a literal ':' in route paths,
  // so the OpenAPI ':accept' / ':reject' suffixes can't be expressed as separate route
  // entries. We register one wildcard route per resource and dispatch on the suffix.
  app.post<{ Params: { '*': string }; Querystring: { commandId?: string }; Body: unknown }>(
    '/app-install-requests/*',
    async (req, reply) => {
      if (!await checkAdmin(cfg, req, reply)) return
      const m = req.params['*'].match(/^(.+):(accept|reject)$/)
      const contractId = m?.[1]
      const action = m?.[2]
      if (contractId === undefined || action === undefined) {
        reply.code(404).send({ message: 'unknown_action' })
        return
      }

      if (action === 'accept') {
        const body = req.body as { installMeta?: { data?: Record<string, string> }; meta?: { data?: Record<string, string> } }
        const res = await licensing.acceptAppInstallRequest(contractId, req.query.commandId, body)
        if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
        reply.code(201).send(res.body)
        return
      }

      const body = req.body as { meta?: { data?: Record<string, string> } }
      const res = await licensing.rejectAppInstallRequest(contractId, req.query.commandId, body)
      if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
      reply.code(204).send()
    }
  )
}
