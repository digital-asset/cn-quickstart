export interface BackendConfig {
  port: number
  registryBaseUri: string
  ledgerHost: string
  ledgerPort: number
  ledgerJsonApiBaseUrl: string
  postgres: { host: string; port: number; database: string; user: string; password: string }
  authMode: 'oauth2' | 'shared-secret'
  testMode: boolean
  appProviderParty: string
  appProviderUserId: string
  // Static bearer token used in shared-secret mode (provided by the onboarding volume).
  sharedSecretToken?: string
  oauth2?: {
    backendClientId: string
    backendClientSecret: string
    backendOidcClientId: string
    issuerUrl: string
    appUserBackendOidcClientId: string
    appUserIssuerUrl: string
  }
}

const required = (name: string): string => {
  const v = process.env[name]
  if (v === undefined || v === '') throw new Error(`Missing env var: ${name}`)
  return v
}

const optional = (name: string): string | undefined => {
  const v = process.env[name]
  return v === undefined || v === '' ? undefined : v
}

export const loadConfig = (): BackendConfig => {
  const ledgerHost = required('LEDGER_HOST')
  const ledgerPort = Number(required('LEDGER_PORT'))
  const authMode: 'oauth2' | 'shared-secret' = required('SPRING_PROFILES_ACTIVE').includes('oauth2')
    ? 'oauth2'
    : 'shared-secret'

  return {
    port: Number(required('BACKEND_PORT')),
    registryBaseUri: required('REGISTRY_BASE_URI'),
    ledgerHost,
    ledgerPort,
    ledgerJsonApiBaseUrl: `http://${ledgerHost}:${ledgerPort}`,
    postgres: {
      host: required('POSTGRES_HOST'),
      port: Number(required('POSTGRES_PORT')),
      database: required('POSTGRES_DATABASE'),
      user: required('POSTGRES_USERNAME'),
      password: required('POSTGRES_PASSWORD')
    },
    authMode,
    testMode: process.env['TEST_MODE'] === 'on',
    appProviderParty: required('APP_PROVIDER_PARTY'),
    // OAuth2: the Keycloak `sub` claim is the user's UUID (AUTH_APP_PROVIDER_BACKEND_USER_ID).
    // Shared-secret: the JWT subject is the username (AUTH_APP_PROVIDER_BACKEND_USER_NAME).
    // The JSON Ledger API rejects requests where commands.userId disagrees with the token's userId claim.
    appProviderUserId: authMode === 'shared-secret'
      ? required('AUTH_APP_PROVIDER_BACKEND_USER_NAME')
      : (optional('AUTH_APP_PROVIDER_BACKEND_USER_ID') ?? 'AppId'),
    sharedSecretToken: authMode === 'shared-secret' ? optional('APP_PROVIDER_BACKEND_USER_TOKEN') : undefined,
    oauth2: authMode === 'oauth2' ? {
      backendClientId: required('AUTH_APP_PROVIDER_BACKEND_CLIENT_ID'),
      backendClientSecret: required('AUTH_APP_PROVIDER_BACKEND_SECRET'),
      backendOidcClientId: required('AUTH_APP_PROVIDER_BACKEND_OIDC_CLIENT_ID'),
      issuerUrl: required('AUTH_APP_PROVIDER_ISSUER_URL'),
      appUserBackendOidcClientId: optional('AUTH_APP_USER_BACKEND_OIDC_CLIENT_ID') ?? '',
      appUserIssuerUrl: optional('AUTH_APP_USER_ISSUER_URL') ?? ''
    } : undefined
  }
}
