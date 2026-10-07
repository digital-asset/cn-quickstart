import type { BackendConfig } from '../config.js'

// Mirrors Spring's `test` profile (OAuth2AuthenticationSuccessHandler): in TEST_MODE the
// JWT's `party_id` claim wins over the tenant-registered party so each integration-test
// run gets a fresh AppUser party. CAUTION: not for production — a forged claim would
// otherwise let any caller act as any party.
export const resolveTestModePartyId = (
  cfg: BackendConfig,
  claims: Record<string, unknown> | undefined,
  fallback: string
): string => {
  if (!cfg.testMode || claims === undefined) return fallback
  const claim = claims['party_id']
  return typeof claim === 'string' && claim !== '' ? claim : fallback
}
