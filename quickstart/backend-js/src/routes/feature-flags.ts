import type { FastifyInstance } from 'fastify'
import type { BackendConfig } from '../config.js'

export const registerFeatureFlags = async (app: FastifyInstance, cfg: BackendConfig): Promise<void> => {
  app.get('/feature-flags', async () => ({
    authMode: cfg.authMode
  }))
}
