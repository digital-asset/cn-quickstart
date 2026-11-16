import { expect, test } from 'bun:test';
import { createHash, generateKeyPairSync, sign } from 'node:crypto';
import { createApp } from '../src/app';
import { readApplicationConfig } from '../src/config/application';
import { appDependencies } from './fixtures/dependencies';

test('Only a verified, single-use AppProvider callback opens an admin session', async () => {
  const trusted = generateKeyPairSync('rsa', { modulusLength: 2048 });
  const other = generateKeyPairSync('rsa', { modulusLength: 2048 });
  const publicKey = {
    ...trusted.publicKey.export({ format: 'jwk' }),
    kid: 'provider',
    alg: 'RS256',
  };
  const codes = new Map<string, { nonce: string; challenge: string; wrongKey: boolean }>();
  let exchanges = 0;
  let advertiseLogout = false;
  let issuedIdToken = '';
  const server = Bun.serve({
    hostname: '127.0.0.1',
    port: 0,
    /** Serves discovery, signing keys, and the token exchange for the test provider. */
    async fetch(request) {
      const { origin, pathname } = new URL(request.url);
      if (pathname === '/.well-known/openid-configuration')
        return Response.json({
          issuer: origin,
          authorization_endpoint: `${origin}/authorize`,
          token_endpoint: `${origin}/token`,
          ...(advertiseLogout ? { end_session_endpoint: `${origin}/end-session` } : {}),
          jwks_uri: `${origin}/keys`,
        });
      if (pathname === '/keys') return Response.json({ keys: [publicKey] });
      if (pathname !== '/token') return new Response(null, { status: 404 });
      exchanges++;
      const form = new URLSearchParams(await request.text());
      const code = form.get('code') ?? '';
      const grant = codes.get(code);
      codes.delete(code);
      if (
        !grant ||
        form.get('client_id') !== 'browser' ||
        form.get('redirect_uri') !== 'https://app.example/login/oauth2/code/AppProvider' ||
        createHash('sha256')
          .update(form.get('code_verifier') ?? '')
          .digest('base64url') !== grant.challenge
      )
        return Response.json({ error: 'invalid_grant' }, { status: 400 });
      const now = Math.floor(Date.now() / 1000);
      const payload = [
        { alg: 'RS256', kid: 'provider' },
        {
          iss: origin,
          aud: 'browser',
          sub: 'alice',
          name: 'Alice',
          iat: now,
          exp: now + 60,
          nonce: grant.nonce,
          party_id: 'test-party',
        },
      ]
        .map((part) => Buffer.from(JSON.stringify(part)).toString('base64url'))
        .join('.');
      const signature = sign(
        'RSA-SHA256',
        Buffer.from(payload),
        grant.wrongKey ? other.privateKey : trusted.privateKey,
      );
      issuedIdToken = `${payload}.${signature.toString('base64url')}`;
      return Response.json({
        token_type: 'Bearer',
        access_token: 'test-access',
        id_token: issuedIdToken,
      });
    },
  });
  try {
    for (const scenario of [
      'success',
      'profile',
      'state',
      'nonce',
      'signature',
      'pkce',
      'missing-cookie',
    ]) {
      advertiseLogout = scenario === 'success';
      const application = readApplicationConfig({
        AUTH_MODE: 'oauth2',
        APP_PROVIDER_PARTY: 'configured-party',
        AUTH_APP_PROVIDER_ISSUER_URL: server.url.origin,
        AUTH_APP_PROVIDER_BACKEND_OIDC_CLIENT_ID: 'browser',
        OIDC_PUBLIC_ORIGIN: 'https://app.example',
        LEDGER_OAUTH_ALLOW_HTTP: 'true',
        TEST_MODE: scenario === 'profile' ? 'on' : 'off',
      });
      const app = createApp({ ...appDependencies, application });
      /** Sends a browser request through the app with the cookie from the previous step. */
      const get = (path: string, cookie = '') =>
        app.handle(new Request(`http://internal${path}`, { headers: { cookie } }));
      const login = await get('/oauth2/authorization/AppProvider');
      const cookie = (login.headers.get('set-cookie') ?? '').split(';')[0];
      const params = new URL(login.headers.get('location') ?? '').searchParams;
      const code = crypto.randomUUID();
      codes.set(code, {
        nonce: scenario === 'nonce' ? 'wrong' : (params.get('nonce') ?? ''),
        challenge: scenario === 'pkce' ? 'wrong' : (params.get('code_challenge') ?? ''),
        wrongKey: scenario === 'signature',
      });
      const callback = `/login/oauth2/code/AppProvider?${new URLSearchParams({ code, state: scenario === 'state' ? 'wrong' : (params.get('state') ?? '') })}`;
      const before = exchanges;
      const response = await get(callback, scenario === 'missing-cookie' ? '' : cookie);
      expect(response.status).toBe(302);
      const session = response.headers
        .getSetCookie()
        .find((header) => header.startsWith('JSESSIONID='))
        ?.split(';')[0];
      if (scenario === 'success' || scenario === 'profile') {
        expect(response.headers.get('location')).toBe('/');
        expect(session).toBeTruthy();
        const party = scenario === 'profile' ? 'test-party' : 'configured-party';
        const user = await get('/user', session);
        expect(user.status).toBe(200);
        expect(await user.json()).toEqual({
          name: 'Alice',
          party,
          isAdmin: true,
          roles: ['ROLE_ADMIN', 'AppProvider', party],
          walletUrl: null,
        });

        // The browser session grants admin access, but cannot rescue an invalid bearer.
        const path = '/admin/tenant-registrations';
        const listed = await get(path, session);
        expect(listed.status).toBe(200);
        expect(await listed.json()).toEqual([
          {
            tenantId: 'AppProvider',
            partyId: 'configured-party',
            internal: true,
            clientId: 'browser',
            issuerUrl: server.url.href,
            walletUrl: null,
            users: null,
          },
        ]);
        const denied = await app.handle(
          new Request(`http://internal${path}`, {
            headers: { cookie: session ?? '', authorization: 'Bearer invalid' },
          }),
        );
        expect(denied.status).toBe(401);
        const xsrf = response.headers
          .getSetCookie()
          .find((header) => header.startsWith('XSRF-TOKEN='));
        expect(xsrf).toBeTruthy();
        expect(xsrf).not.toContain('HttpOnly');
        const csrfCookie = xsrf?.split(';')[0] ?? '';
        const token = csrfCookie.slice('XSRF-TOKEN='.length);
        /** Reproduces the frontend's empty JSON POST with the browser cookies. */
        const logout = (cookie: string, csrf?: string) =>
          app.handle(
            new Request('http://internal/logout', {
              method: 'POST',
              headers: {
                cookie,
                'content-type': 'application/json',
                ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {}),
              },
            }),
          );
        const cookies = `${session}; ${csrfCookie}`;
        /** Registers a tenant using the browser cookies and optional CSRF header. */
        const register = (csrf?: string) =>
          app.handle(
            new Request('http://internal/admin/tenant-registrations', {
              method: 'POST',
              headers: {
                cookie: cookies,
                'content-type': 'application/json',
                ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {}),
              },
              body: JSON.stringify({
                tenantId: 'AppUser',
                partyId: 'customer',
                clientId: 'another-client',
                issuerUrl: server.url.origin,
              }),
            }),
          );
        expect((await register()).status).toBe(403);
        expect((await register('wrong')).status).toBe(403);
        expect((await register(token)).status).toBe(201);
        for (const denied of [
          await logout(cookies),
          await logout(cookies, 'incorrect'),
          await logout(session ?? '', token),
          await logout(`${session}; XSRF-TOKEN=other`, 'other'),
        ]) {
          expect(denied.status).toBe(403);
          expect(denied.headers.has('set-cookie')).toBe(false);
        }
        expect((await get('/user', session)).status).toBe(200);
        const ended = await logout(cookies, token);
        expect(ended.status).toBe(302);
        if (advertiseLogout) {
          const target = new URL(ended.headers.get('location') ?? '');
          expect(target.origin + target.pathname).toBe(`${server.url.origin}/end-session`);
          expect(target.searchParams.get('id_token_hint')).toBe(issuedIdToken);
          expect(target.searchParams.has('post_logout_redirect_uri')).toBe(false);
        } else expect(ended.headers.get('location')).toBe('/');
        for (const name of ['JSESSIONID', 'XSRF-TOKEN']) {
          expect(
            ended.headers.getSetCookie().find((header) => header.startsWith(`${name}=`)),
          ).toContain('Max-Age=0');
        }
        expect((await get('/user', session)).status).toBe(401);
      } else {
        expect(response.headers.get('location')).toBe('/login?error');
        expect(session).toBeUndefined();
        expect((await get('/user', cookie)).status).toBe(401);
      }
      expect(exchanges - before).toBe(['missing-cookie', 'state'].includes(scenario) ? 0 : 1);
      // Replaying a consumed attempt must fail before the provider is contacted.
      if (scenario !== 'missing-cookie') {
        const count = exchanges;
        expect((await get(callback, cookie)).headers.get('location')).toBe('/login?error');
        expect(exchanges).toBe(count);
      }
    }
  } finally {
    server.stop(true);
  }
});
