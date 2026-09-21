import type { FastifyInstance } from 'fastify'
import * as openid from 'openid-client'
import { randomBytes } from 'node:crypto'
import type { BackendConfig } from '../config.js'
import type { TenantRepository } from '../tenants/repository.js'
import { OAuth2Registry } from './oauth2-registry.js'
import { XSRF_COOKIE } from './cookies.js'
import { resolveTestModePartyId } from './test-mode-party.js'

const baseUrl = (req: { headers: Record<string, string | string[] | undefined> }, cfg: BackendConfig): string => {
  const proto = (req.headers['x-forwarded-proto'] as string | undefined) ?? 'http'
  const host = (req.headers['x-forwarded-host'] as string | undefined) ?? `localhost:${cfg.port}`
  return `${proto}://${host}`
}

export const initOAuth2Registry = async (cfg: BackendConfig): Promise<OAuth2Registry> => {
  const registry = new OAuth2Registry()
  if (cfg.authMode !== 'oauth2' || cfg.oauth2 === undefined) return registry
  await registry.register('AppProvider', cfg.oauth2.backendOidcClientId, cfg.oauth2.issuerUrl)
  return registry
}

export const registerOAuth2 = async (app: FastifyInstance, cfg: BackendConfig, registry: OAuth2Registry, tenants: TenantRepository): Promise<void> => {
  if (cfg.authMode !== 'oauth2') return

  app.get<{ Params: { registrationId: string } }>('/oauth2/authorization/:registrationId', async (req, reply) => {
    const entry = registry.get(req.params.registrationId)
    if (entry === undefined) { reply.code(404); return { message: 'unknown_registration' } }
    const codeVerifier = openid.randomPKCECodeVerifier()
    const codeChallenge = await openid.calculatePKCECodeChallenge(codeVerifier)
    const state = randomBytes(16).toString('hex')
    const nonce = randomBytes(16).toString('hex')
    req.session.oauthState = { state, codeVerifier, registrationId: req.params.registrationId, nonce }
    const redirectUri = `${baseUrl(req, cfg)}/login/oauth2/code/${req.params.registrationId}`
    const url = openid.buildAuthorizationUrl(entry.config, {
      redirect_uri: redirectUri,
      scope: 'openid',
      code_challenge: codeChallenge,
      code_challenge_method: 'S256',
      state,
      nonce
    })
    return reply.redirect(url.href)
  })

  app.get<{ Params: { registrationId: string }; Querystring: Record<string, string> }>(
    '/login/oauth2/code/:registrationId',
    async (req, reply) => {
      const entry = registry.get(req.params.registrationId)
      const stored = req.session.oauthState
      if (entry === undefined || stored === undefined || stored.registrationId !== req.params.registrationId) {
        reply.code(400); return { message: 'invalid_state' }
      }
      // Use the raw request URL to preserve the exact encoding of query parameters.
      const currentUrl = new URL(req.raw.url ?? req.url, baseUrl(req, cfg))
      const tokens = await openid.authorizationCodeGrant(entry.config, currentUrl, {
        pkceCodeVerifier: stored.codeVerifier,
        expectedState: stored.state,
        expectedNonce: stored.nonce
      })
      const rawClaims = tokens.claims()
      const claims: Record<string, unknown> = rawClaims !== undefined ? (rawClaims as Record<string, unknown>) : {}
      const name = (claims['name'] as string | undefined) ??
        (claims['preferred_username'] as string | undefined) ??
        (claims['sub'] as string | undefined) ?? 'unknown'
      const isAppProvider = entry.tenantId === 'AppProvider'
      const roles: string[] = isAppProvider
        ? ['ROLE_ADMIN', 'SCOPE_openid']
        : ['ROLE_USER', 'SCOPE_openid']
      const tenantPartyId = tenants.get(entry.tenantId)?.partyId ?? ''
      req.session.user = {
        name,
        tenantId: entry.tenantId,
        partyId: resolveTestModePartyId(cfg, claims, tenantPartyId),
        userId: claims['sub'] as string | undefined,
        idTokenClaims: claims,
        roles,
        isAdmin: isAppProvider
      }
      req.session.oauthState = undefined
      // Mirrors Spring Security's CSRF workaround for oauth2 callback: ensure an XSRF-TOKEN
      // cookie exists, but do not overwrite a token the client already holds (preserves
      // tokens from in-flight tabs that were issued before login).
      if (req.cookies[XSRF_COOKIE] === undefined || req.cookies[XSRF_COOKIE] === '') {
        const csrfToken = randomBytes(32).toString('hex')
        reply.setCookie(XSRF_COOKIE, csrfToken, { httpOnly: false, sameSite: 'lax', path: '/', secure: false })
      }
      return reply.redirect('/')
    }
  )
}
