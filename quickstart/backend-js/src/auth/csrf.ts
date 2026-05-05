import type { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify'
import { randomBytes } from 'node:crypto'
import { XSRF_COOKIE, XSRF_HEADER } from './cookies.js'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

const ensureToken = (req: FastifyRequest, reply: FastifyReply): string => {
  const existing = req.cookies[XSRF_COOKIE]
  if (existing !== undefined && existing !== '') return existing
  const fresh = randomBytes(32).toString('hex')
  reply.setCookie(XSRF_COOKIE, fresh, { httpOnly: false, sameSite: 'lax', path: '/', secure: false })
  return fresh
}

export const registerCsrf = async (app: FastifyInstance): Promise<void> => {
  app.addHook('onRequest', async (req, reply) => {
    if (SAFE_METHODS.has(req.method)) {
      ensureToken(req, reply)
      return
    }
    // Bearer-authenticated calls are stateless service-to-service traffic (e.g.
    // register-app-user-tenant); CSRF is a session-cookie defense and doesn't apply.
    // Mirrors Spring Security's default of bypassing CSRF for JWT bearer auth.
    const authHeader = req.headers['authorization']
    if (typeof authHeader === 'string' && /^Bearer\s+\S+/i.test(authHeader)) return
    const cookieToken = req.cookies[XSRF_COOKIE]
    const headerToken = req.headers[XSRF_HEADER]
    if (cookieToken === undefined || headerToken === undefined || cookieToken !== headerToken) {
      return reply.code(403).send({ message: 'csrf_mismatch' })
    }
  })
}
