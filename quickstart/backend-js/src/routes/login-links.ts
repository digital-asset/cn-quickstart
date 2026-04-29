import type { FastifyInstance } from 'fastify'
import type { BackendConfig } from '../config.js'
import type { OAuth2Registry } from '../auth/oauth2-registry.js'

export const registerLoginLinks = async (app: FastifyInstance, cfg: BackendConfig, oauth2Registry: OAuth2Registry): Promise<void> => {
  app.get('/login-links', async () => {
    if (cfg.authMode !== 'oauth2') return []
    return oauth2Registry.list().map((entry) => ({
      name: entry.tenantId,
      url: oauth2Registry.loginUrl(entry.registrationId)
    }))
  })
}
