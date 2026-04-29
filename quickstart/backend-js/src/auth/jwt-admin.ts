import { createRemoteJWKSet, jwtVerify } from 'jose'
import type { BackendConfig } from '../config.js'

const jwksCache = new Map<string, ReturnType<typeof createRemoteJWKSet>>()

const getJwks = (issuer: string): ReturnType<typeof createRemoteJWKSet> => {
  const existing = jwksCache.get(issuer)
  if (existing !== undefined) return existing
  const jwksUri = `${issuer.replace(/\/$/, '')}/protocol/openid-connect/certs`
  const jwks = createRemoteJWKSet(new URL(jwksUri))
  jwksCache.set(issuer, jwks)
  return jwks
}

export const verifyAdminJwt = async (cfg: BackendConfig, authHeader: string | undefined): Promise<boolean> => {
  if (cfg.authMode !== 'oauth2' || cfg.oauth2 === undefined) return false
  if (authHeader === undefined || !authHeader.startsWith('Bearer ')) return false
  const token = authHeader.slice(7)
  const issuer = cfg.oauth2.issuerUrl
  if (issuer === '') return false
  try {
    await jwtVerify(token, getJwks(issuer), { issuer })
    return true
  } catch {
    return false
  }
}
