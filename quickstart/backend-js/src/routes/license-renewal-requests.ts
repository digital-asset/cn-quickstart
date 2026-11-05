import type { FastifyInstance } from 'fastify'
import type { Services } from '../http/app.js'
import { checkAdmin } from '../auth/admin-gate.js'

export const registerLicenseRenewalRequests = async (app: FastifyInstance, services: Services): Promise<void> => {
  const { cfg, licensing } = services

  // See app-install-requests.ts for the rationale behind the wildcard + suffix dispatch.
  // Even with a single action, the literal-colon limitation in find-my-way prevents using
  // ':contractId\\:withdraw' directly — the param name gets mangled to 'contractId:withdraw'.
  app.post<{ Params: { '*': string }; Querystring: { commandId?: string } }>(
    '/license-renewal-requests/*',
    async (req, reply) => {
      if (!await checkAdmin(cfg, req, reply)) return
      const m = req.params['*'].match(/^(.+):(withdraw)$/)
      const contractId = m?.[1]
      if (contractId === undefined) {
        reply.code(404).send({ message: 'unknown_action' })
        return
      }
      const res = await licensing.withdrawLicenseRenewalRequest(contractId, req.query.commandId)
      if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
      reply.code(204).send()
    }
  )
}
