import Fastify, { type FastifyInstance } from 'fastify'
import formbody from '@fastify/formbody'
import type pg from 'pg'
import type { BackendConfig } from '../config.js'
import { registerSession } from '../auth/session.js'
import { registerCsrf } from '../auth/csrf.js'
import { registerOAuth2 } from '../auth/oauth2.js'
import { registerSharedSecret, registerLogout } from '../auth/shared-secret.js'
import type { OAuth2Registry } from '../auth/oauth2-registry.js'
import type { TenantRepository } from '../tenants/repository.js'
import type { LedgerApi } from '../canton/ledger.js'
import type { TokenStandardClient } from '../token-standard/client.js'
import type { LicensingService } from '../domain/licensing/service.js'
import { registerAdmin } from '../routes/admin.js'
import { registerLoginLinks } from '../routes/login-links.js'
import { registerUser } from '../routes/user.js'
import { registerFeatureFlags } from '../routes/feature-flags.js'
import { registerAppInstallRequests } from '../routes/app-install-requests.js'
import { registerAppInstalls } from '../routes/app-installs.js'
import { registerLicenses } from '../routes/licenses.js'
import { registerLicenseRenewalRequests } from '../routes/license-renewal-requests.js'

export interface Services {
  cfg: BackendConfig
  pool: pg.Pool
  ledger: LedgerApi
  tokenStandard: TokenStandardClient
  tenants: TenantRepository
  oauth2Registry: OAuth2Registry
  licensing: LicensingService
}

export const buildApp = async (services: Services): Promise<FastifyInstance> => {
  const { cfg, tenants, oauth2Registry } = services
  const app = Fastify({ logger: { level: 'info' }, trustProxy: true })
  // Override Fastify's default JSON parser to accept empty bodies. The frontend's logout
  // sends `Content-Type: application/json` with no body; the default parser rejects with
  // "Body cannot be empty when content-type is set to 'application/json'".
  app.addContentTypeParser('application/json', { parseAs: 'string' }, (_req, body, done) => {
    if (typeof body !== 'string' || body.length === 0) { done(null, undefined); return }
    try {
      done(null, JSON.parse(body))
    } catch (err) {
      const e = err as Error & { statusCode?: number }
      e.statusCode = 400
      done(e)
    }
  })
  await app.register(formbody)
  await registerSession(app)
  if (cfg.authMode === 'oauth2') await registerCsrf(app)
  await registerOAuth2(app, cfg, oauth2Registry, tenants)
  await registerSharedSecret(app, cfg, tenants)
  await registerLogout(app)

  await registerAdmin(app, cfg, tenants, oauth2Registry)
  await registerLoginLinks(app, cfg, oauth2Registry)
  await registerUser(app, tenants)
  await registerFeatureFlags(app, cfg)
  await registerAppInstallRequests(app, services)
  await registerAppInstalls(app, services)
  await registerLicenses(app, services)
  await registerLicenseRenewalRequests(app, services)

  app.get('/livez', async () => ({ status: 'ok' }))
  app.get('/health', async () => ({ status: 'ok', backend: 'js', authMode: cfg.authMode }))

  app.setErrorHandler((err: unknown, req, reply) => {
    const e = err as { statusCode?: number; message?: string }
    const status = e.statusCode ?? 500
    if (status >= 500) req.log.error({ err }, 'unhandled error')
    reply.code(status).send({ message: e.message ?? 'Unknown error' })
  })

  return app
}
