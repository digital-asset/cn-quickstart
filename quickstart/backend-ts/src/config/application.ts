import { AuthMode, readAuthMode, readProviderIssuer } from './auth-mode';

export type ApplicationConfig = ReturnType<typeof readApplicationConfig>;

/** Loads the AppProvider tenant and validates the settings for the selected authentication mode. */
export function readApplicationConfig(
  env: Readonly<Record<string, string | undefined>> = process.env,
) {
  const appProviderParty = env.APP_PROVIDER_PARTY?.trim();
  if (!appProviderParty) {
    throw new Error('APP_PROVIDER_PARTY is required for the Quickstart application');
  }

  const appProvider = {
    tenantId: 'AppProvider',
    internal: true,
    partyId: appProviderParty,
  } as const;
  const authMode = readAuthMode(env);
  if (authMode === AuthMode.SharedSecret) {
    return { appProvider: { ...appProvider, users: ['app-provider'] }, authMode };
  }

  const { issuer, allowHttp } = readProviderIssuer(env);
  const clientId = env.AUTH_APP_PROVIDER_BACKEND_OIDC_CLIENT_ID?.trim();
  if (!clientId)
    throw new Error('AUTH_APP_PROVIDER_BACKEND_OIDC_CLIENT_ID is required for web login');
  const publicOrigin = new URL(
    env.OIDC_PUBLIC_ORIGIN ?? `http://app-provider.localhost:${env.APP_PROVIDER_UI_PORT ?? '3000'}`,
  );
  if (
    !['https:', 'http:'].includes(publicOrigin.protocol) ||
    publicOrigin.username ||
    publicOrigin.password ||
    publicOrigin.href !== `${publicOrigin.origin}/`
  ) {
    throw new Error('OIDC_PUBLIC_ORIGIN must be an HTTP or HTTPS origin');
  }
  return {
    appProvider,
    authMode,
    oidc: {
      issuer,
      clientId,
      testProfile:
        env.TEST_MODE === 'on' ||
        env.SPRING_PROFILES_ACTIVE?.split(',').some((profile) => profile.trim() === 'test') ===
          true,
      allowHttp,
      publicOrigin: publicOrigin.origin,
    },
  };
}
