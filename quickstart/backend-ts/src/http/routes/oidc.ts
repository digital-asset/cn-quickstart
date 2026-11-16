import { Elysia, t } from 'elysia';
import * as oidc from 'openid-client';
import { createAdminBearerVerifier } from '../../auth/admin-bearer';
import { Sessions } from '../../auth/sessions';
import type { ApplicationConfig } from '../../config/application';

import { oidcTenantRoutes } from './oidc-tenants';

const loginLifetimeSeconds = 300;

/** Registers the AppProvider login, callback, and session-backed user endpoints. */
export function oidcRoutes(
  provider: ApplicationConfig['appProvider'],
  options: NonNullable<ApplicationConfig['oidc']>,
) {
  const registrationId = provider.tenantId;
  const sessions = new Sessions<{
    name: string | null;
    party: string;
    csrf: string;
    idToken: string;
  }>();
  const cookieOptions = {
    path: '/',
    sameSite: 'lax',
    secure: options.publicOrigin.startsWith('https:'),
  } as const;
  const callbackPath = `/login/oauth2/code/${registrationId}`;
  const loginPath = `/oauth2/authorization/${registrationId}`;
  const redirectUri = new URL(callbackPath, options.publicOrigin).href;
  const attempts = new Map<
    string,
    { state: string; nonce: string; verifier: string; expires: number }
  >();
  let configuration: Promise<oidc.Configuration> | undefined;

  let verifyBearer: ReturnType<typeof createAdminBearerVerifier> | undefined;

  /** Loads provider metadata for a public OIDC client. HTTP requires explicit opt-in. */
  function discoverClient(issuer: URL, clientId: string) {
    return oidc.discovery(issuer, clientId, { token_endpoint_auth_method: 'none' }, oidc.None(), {
      timeout: 10,
      execute: [
        oidc.enableNonRepudiationChecks,
        ...(issuer.protocol === 'http:' && options.allowHttp ? [oidc.allowInsecureRequests] : []),
      ],
    });
  }

  /** Shares discovery between browser login and bearer requests, retrying after failures. */
  function discover() {
    configuration ??= discoverClient(options.issuer, options.clientId).catch((error) => {
      configuration = undefined;
      throw error;
    });
    return configuration;
  }

  return new Elysia({ name: 'oidc' })
    .use(
      oidcTenantRoutes(
        provider,
        options,
        discoverClient,
        async (request, sessionId, csrfCookie) => {
          const authorization = request.headers.get('authorization');
          if (authorization !== null) {
            const config = await discover();
            verifyBearer ??= createAdminBearerVerifier(config.serverMetadata(), provider);
            return (await verifyBearer(authorization)) ? 200 : 401;
          }
          const session = sessions.read(sessionId);
          if (!session) return 401;
          if (
            request.method === 'POST' &&
            (session.csrf !== csrfCookie || session.csrf !== request.headers.get('x-xsrf-token'))
          )
            return 403;
          return 200;
        },
      ),
    )
    .get('/login-links', () => [{ name: registrationId, url: loginPath }], {
      response: t.Array(t.Object({ name: t.String(), url: t.String() })),
      detail: { operationId: 'listLinks', tags: ['Auth'] },
    })
    .get(loginPath, async ({ cookie: { OIDC_LOGIN }, redirect, set, status }) => {
      try {
        const config = await discover();
        // Only the PKCE challenge travels in the redirect. The verifier stays here for the code exchange.
        const verifier = oidc.randomPKCECodeVerifier();
        const state = oidc.randomState();
        const nonce = oidc.randomNonce();
        const url = oidc.buildAuthorizationUrl(config, {
          response_type: 'code',
          scope: 'openid',
          redirect_uri: redirectUri,
          state,
          nonce,
          code_challenge: await oidc.calculatePKCECodeChallenge(verifier),
          code_challenge_method: 'S256',
        });
        // The callback cookie holds an opaque ID for this pending attempt, which expires after five minutes.
        const id = crypto.randomUUID();
        attempts.set(id, {
          state,
          nonce,
          verifier,
          expires: Date.now() + loginLifetimeSeconds * 1000,
        });
        setTimeout(() => attempts.delete(id), loginLifetimeSeconds * 1000).unref();
        OIDC_LOGIN.set({
          value: id,
          httpOnly: true,
          secure: options.publicOrigin.startsWith('https:'),
          sameSite: 'lax',
          path: '/login/oauth2/code',
          maxAge: loginLifetimeSeconds,
        });
        set.headers['cache-control'] = 'no-store';
        return redirect(url.href, 302);
      } catch (error) {
        console.error(
          'OIDC authorization could not start',
          error instanceof Error ? error.name : 'unknown error',
        );
        return status(500, { message: 'Internal Server Error' });
      }
    })
    .get(
      callbackPath,
      async ({ request, cookie: { OIDC_LOGIN, JSESSIONID, 'XSRF-TOKEN': xsrf }, redirect }) => {
        const id = OIDC_LOGIN.value;
        const attempt = typeof id === 'string' ? attempts.get(id) : undefined;
        if (typeof id === 'string') attempts.delete(id);
        OIDC_LOGIN.set({
          value: '',
          path: '/login/oauth2/code',
          httpOnly: true,
          maxAge: 0,
          sameSite: 'lax',
          secure: options.publicOrigin.startsWith('https:'),
        });
        if (!attempt || attempt.expires <= Date.now() || !configuration)
          return redirect('/login?error', 302);

        // Consume the attempt before the exchange so concurrent callbacks cannot reuse it.
        const url = new URL(redirectUri);
        url.search = new URL(request.url).search;
        try {
          const tokens = await oidc.authorizationCodeGrant(await configuration, url, {
            expectedState: attempt.state,
            expectedNonce: attempt.nonce,
            pkceCodeVerifier: attempt.verifier,
            idTokenExpected: true,
          });
          const claims = tokens.claims();
          if (!claims || !tokens.id_token) return redirect('/login?error', 302);
          const party =
            options.testProfile && typeof claims.party_id === 'string'
              ? claims.party_id
              : provider.partyId;
          // The frontend echoes the readable cookie in X-XSRF-TOKEN on logout.
          const csrf = crypto.randomUUID();
          const session = sessions.open(
            {
              name: typeof claims.name === 'string' ? claims.name : null,
              party,
              csrf,
              idToken: tokens.id_token,
            },
            JSESSIONID.value,
          );
          JSESSIONID.set({ ...cookieOptions, value: session, httpOnly: true });
          xsrf.set({ ...cookieOptions, value: csrf, httpOnly: false });
          return redirect('/', 302);
        } catch (error) {
          console.warn(
            'OIDC callback rejected',
            error instanceof Error ? error.name : 'unknown error',
          );
          return redirect('/login?error', 302);
        }
      },
    )
    .get(
      '/user',
      ({ cookie: { JSESSIONID }, status }) => {
        const user = sessions.read(JSESSIONID.value);
        if (!user) return status(401, null);
        return {
          name: user.name,
          party: user.party,
          roles: ['ROLE_ADMIN', registrationId, user.party],
          isAdmin: true,
          walletUrl: null,
        };
      },
      { detail: { operationId: 'getAuthenticatedUser', tags: ['Auth'] } },
    )
    .post(
      '/logout',
      async ({ cookie: { JSESSIONID, 'XSRF-TOKEN': xsrf }, headers, redirect, status }) => {
        const session = sessions.read(JSESSIONID.value);
        if (!session || xsrf.value !== session.csrf || headers['x-xsrf-token'] !== session.csrf) {
          return status(403, null);
        }
        // Revoke local access even when the provider has no logout endpoint.
        sessions.invalidate(JSESSIONID.value);
        JSESSIONID.set({ ...cookieOptions, value: '', httpOnly: true, maxAge: 0 });
        xsrf.set({ ...cookieOptions, value: '', httpOnly: false, maxAge: 0 });
        const config = await configuration;
        if (!config?.serverMetadata().end_session_endpoint) return redirect('/', 302);
        return redirect(
          oidc.buildEndSessionUrl(config, { id_token_hint: session.idToken }).href,
          302,
        );
      },
      { parse: 'none' },
    );
}
