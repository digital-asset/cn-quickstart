import type { FastifyInstance } from 'fastify'
import type { Services } from '../http/app.js'
import { checkAdmin } from '../auth/admin-gate.js'

export const registerAppInstalls = async (app: FastifyInstance, services: Services): Promise<void> => {
  const { cfg, licensing } = services

  app.get('/app-installs', async (req, reply) => {
    if (req.session.user === undefined) { reply.code(401).send({ message: 'unauthorized' }); return }
    return licensing.listAppInstalls(req.session.user.partyId)
  })

  // See app-install-requests.ts for the rationale behind the wildcard + suffix dispatch.
  app.post<{ Params: { '*': string }; Querystring: { commandId?: string }; Body: unknown }>(
    '/app-installs/*',
    async (req, reply) => {
      if (req.session.user === undefined) { reply.code(401).send({ message: 'unauthorized' }); return }
      const m = req.params['*'].match(/^(.+):(create-license|cancel)$/)
      const contractId = m?.[1]
      const action = m?.[2]
      if (contractId === undefined || action === undefined) {
        reply.code(404).send({ message: 'unknown_action' })
        return
      }

      if (action === 'create-license') {
        if (!await checkAdmin(cfg, req, reply)) return
        const body = req.body as { params?: { meta?: { data?: Record<string, string> } } }
        const res = await licensing.createLicense(contractId, req.query.commandId, body)
        if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
        if (res.status === 403) { reply.code(403).send({ message: res.message }); return }
        reply.code(201).send(res.body)
        return
      }

      const body = req.body as { meta?: { data?: Record<string, string> } }
      const res = await licensing.cancelAppInstall(contractId, req.query.commandId, req.session.user.partyId, body)
      if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
      if (res.status === 403) { reply.code(403).send({ message: res.message }); return }
      reply.code(204).send()
    }
  )
}
