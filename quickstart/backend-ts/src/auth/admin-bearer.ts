import { createRemoteJWKSet, errors, jwtVerify } from 'jose';
import type { ApplicationConfig } from '../config/application';

/** Builds a verifier from the AppProvider's trusted discovery metadata, reusing its JWKS cache. */
export function createAdminBearerVerifier(
  metadata: { issuer: string; jwks_uri?: string },
  provider: ApplicationConfig['appProvider'],
) {
  if (!metadata.jwks_uri) throw new Error('AppProvider discovery is missing jwks_uri');
  const url = new URL(metadata.jwks_uri);
  if (
    url.protocol !== 'https:' &&
    !(url.protocol === 'http:' && new URL(metadata.issuer).protocol === 'http:')
  )
    throw new Error('AppProvider JWKS must use HTTPS unless the issuer uses HTTP');
  const keys = createRemoteJWKSet(url);

  const options = {
    issuer: metadata.issuer,
    algorithms: ['RS256'],
    clockTolerance: 60, // Spring Security's default clock skew for exp and nbf.
  };

  /** Tries the issuer's matching keys during rotation, validating claims with the same options. */
  async function verifyToken(token: string) {
    try {
      await jwtVerify(token, keys, options);
      return true;
    } catch (error) {
      if (!(error instanceof errors.JWKSMultipleMatchingKeys)) throw error;
      for await (const key of error) {
        try {
          await jwtVerify(token, key, options);
          return true;
        } catch (cause) {
          if (!(cause instanceof errors.JWSSignatureVerificationFailed)) throw cause;
        }
      }
      return false;
    }
  }

  /** Returns the configured admin identity for a valid bearer; key service failures propagate. */
  return async function verifyAdminBearer(authorization: string | null) {
    const token = authorization?.match(/^Bearer +([^\s,]+)$/i)?.[1];
    if (!token) return null;
    try {
      if (!(await verifyToken(token))) return null;
      return { tenantId: provider.tenantId, partyId: provider.partyId, isAdmin: true };
    } catch (error) {
      if (
        error instanceof errors.JWTClaimValidationFailed ||
        error instanceof errors.JWTExpired ||
        error instanceof errors.JWSInvalid ||
        error instanceof errors.JWTInvalid ||
        error instanceof errors.JOSEAlgNotAllowed ||
        error instanceof errors.JWKSNoMatchingKey ||
        error instanceof errors.JWSSignatureVerificationFailed
      )
        return null;
      throw error;
    }
  };
}
