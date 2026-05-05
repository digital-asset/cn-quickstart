import type { FastifyInstance } from 'fastify'
import cookie from '@fastify/cookie'
import session from '@fastify/session'
import { randomBytes } from 'node:crypto'
import { SESSION_COOKIE } from './cookies.js'

declare module 'fastify' {
  interface Session {
    user?: { name: string; tenantId: string; partyId: string; userId?: string; idTokenClaims?: Record<string, unknown>; roles?: string[]; isAdmin?: boolean }
    oauthState?: { state: string; codeVerifier: string; registrationId: string; nonce: string }
  }
}

export const registerSession = async (app: FastifyInstance): Promise<void> => {
  await app.register(cookie)
  await app.register(session, {
    secret: process.env['SESSION_SECRET'] ?? randomBytes(32).toString('hex'),
    cookieName: SESSION_COOKIE,
    cookie: { httpOnly: true, sameSite: 'lax', path: '/', secure: false },
    saveUninitialized: false
  })
}
