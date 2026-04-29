import type { FastifyReply, FastifyRequest } from 'fastify'
import type { BackendConfig } from '../config.js'
import { verifyAdminJwt } from './jwt-admin.js'

const hasBearerToken = (authHeader: string | undefined): boolean =>
  typeof authHeader === 'string' && /^Bearer\s+\S+/i.test(authHeader)

// Accepts either a valid Bearer JWT or an admin session cookie.
// On failure, replies with 401 (no auth) or 403 (authenticated but not admin) and returns false.
export const checkAdmin = async (cfg: BackendConfig, req: FastifyRequest, reply: FastifyReply): Promise<boolean> => {
  const sessionIsAdmin = req.session.user?.isAdmin
  if (sessionIsAdmin === true) return true

  const authHeader = req.headers['authorization']
  const authenticatedButNotAdmin = sessionIsAdmin === false || hasBearerToken(authHeader)

  if (cfg.authMode === 'oauth2' && await verifyAdminJwt(cfg, authHeader)) return true

  reply.code(authenticatedButNotAdmin ? 403 : 401).send({ message: authenticatedButNotAdmin ? 'forbidden' : 'unauthorized' })
  return false
}
