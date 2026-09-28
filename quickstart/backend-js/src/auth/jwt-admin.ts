import { createRemoteJWKSet, jwtVerify } from 'jose'
import type { BackendConfig } from '../config.js'

export type AdminBearerResult = 'absent' | 'valid' | 'invalid'

// RFC 6750 §2.1: "Bearer" 1*SP b64token
const BEARER_TOKEN = /^Bearer +([A-Za-z0-9\-._~+/]+=*)$/i

const jwksCache = new Map<string, ReturnType<typeof createRemoteJWKSet>>()

const getJwks = (issuer: string): ReturnType<typeof createRemoteJWKSet> => {
  const existing = jwksCache.get(issuer)
  if (existing !== undefined) {
    return existing
  }

  const jwksUri = `${issuer.replace(/\/$/, '')}/protocol/openid-connect/certs`
  const jwks = createRemoteJWKSet(new URL(jwksUri))

  jwksCache.set(issuer, jwks)
  return jwks
}

export const hasBearerToken = (authHeader: string | undefined): authHeader is string =>
  typeof authHeader === 'string' && /^Bearer\s+\S+/i.test(authHeader)

// 'absent' when no Bearer token applies: none was sent, or not in oauth2 mode.
export const checkAdminBearer = async (cfg: BackendConfig, authHeader: string | undefined): Promise<AdminBearerResult> => {
  if (cfg.authMode !== 'oauth2' || !hasBearerToken(authHeader)) {
    return 'absent'
  }

  const token = authHeader.match(BEARER_TOKEN)?.[1]
  const issuer = cfg.oauth2?.issuerUrl

  if (token === undefined || issuer === undefined || issuer === '') {
    return 'invalid'
  }

  try {
    await jwtVerify(token, getJwks(issuer), { issuer })
    return 'valid'
  } catch {
    return 'invalid'
  }
}
