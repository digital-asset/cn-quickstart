import type { FastifyReply, FastifyRequest } from 'fastify'
import type { BackendConfig } from '../config.js'
import { checkAdminBearer } from './jwt-admin.js'

// In oauth2 mode a Bearer token decides on its own: valid passes, anything else is 401.
// Otherwise the session decides: admin passes, non-admin is 403, no session is 401.
// On failure, replies and returns false.
export const checkAdmin = async (cfg: BackendConfig, req: FastifyRequest, reply: FastifyReply): Promise<boolean> => {
  const bearer = await checkAdminBearer(cfg, req.headers['authorization'])
  if (bearer === 'valid') {
    return true
  }

  if (bearer === 'invalid') {
    reply.code(401).send({ message: 'unauthorized' })
    return false
  }

  const sessionIsAdmin = req.session.user?.isAdmin

  if (sessionIsAdmin === true) {
    return true
  }

  if (sessionIsAdmin === false) {
    reply.code(403).send({ message: 'forbidden' })
    return false
  }

  reply.code(401).send({ message: 'unauthorized' })
  return false
}
