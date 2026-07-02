import type { FastifyInstance } from 'fastify'
import type { Services } from '../http/app.js'
import { checkAdmin } from '../auth/admin-gate.js'

export const registerLicenses = async (app: FastifyInstance, services: Services): Promise<void> => {
  const { cfg, licensing } = services

  app.get('/licenses', async (req, reply) => {
    if (req.session.user === undefined) { reply.code(401).send({ message: 'unauthorized' }); return }
    return licensing.listLicenses(req.session.user.partyId)
  })

  // See app-install-requests.ts for the rationale behind the wildcard + suffix dispatch.
  app.post<{ Params: { '*': string }; Querystring: { commandId?: string }; Body: unknown }>(
    '/licenses/*',
    async (req, reply) => {
      if (req.session.user === undefined) { reply.code(401).send({ message: 'unauthorized' }); return }
      const m = req.params['*'].match(/^(.+):(renew|complete-renewal|expire)$/)
      const contractId = m?.[1]
      const action = m?.[2]
      if (contractId === undefined || action === undefined) {
        reply.code(404).send({ message: 'unknown_action' })
        return
      }

      if (action === 'renew') {
        if (!await checkAdmin(cfg, req, reply)) return
        const body = req.body as {
          licenseFeeCc: number
          licenseExtensionDuration: string
          prepareUntilDuration: string
          settleBeforeDuration: string
          description: string
        }
        const res = await licensing.renewLicense(contractId, req.query.commandId, body)
        if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
        reply.code(201).send()
        return
      }

      if (action === 'complete-renewal') {
        if (!await checkAdmin(cfg, req, reply)) return
        const body = req.body as { renewalRequestContractId: string; allocationContractId: string }
        const res = await licensing.completeLicenseRenewal(contractId, req.query.commandId, body)
        if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
        return res.body
      }

      const body = req.body as { meta?: { data?: Record<string, string> } }
      const res = await licensing.expireLicense(contractId, req.query.commandId, req.session.user.partyId, body)
      if (res.status === 404) { reply.code(404).send({ message: res.message }); return }
      return res.body
    }
  )
}
