import { expect, test } from 'bun:test';
import { createHash, generateKeyPairSync, sign } from 'node:crypto';
import { createApp } from '../src/app';
import { readApplicationConfig } from '../src/config/application';
import { appDependencies } from './fixtures/dependencies';

test('Verified OIDC callbacks open single-use login flows with tenant-specific permissions', async () => {
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
  let pauseExchange: (() => Promise<void>) | undefined;
  const server = Bun.serve({
    hostname: '127.0.0.1',
    port: 0,
    /** Serves discovery, signing keys, and the token exchange for the test provider. */
    async fetch(request) {
      const { origin, pathname } = new URL(request.url);
      const external = pathname.startsWith('/customer/');
      const issuer = external ? `${origin}/customer` : origin;
      const clientId = external ? 'another-client' : 'browser';
      const registrationId = external ? 'AppUser-another-client' : 'AppProvider';
      if (pathname === new URL(`${issuer}/.well-known/openid-configuration`).pathname)
        return Response.json({
          issuer,
          authorization_endpoint: `${issuer}/authorize`,
          token_endpoint: `${issuer}/token`,
          ...(advertiseLogout ? { end_session_endpoint: `${issuer}/end-session` } : {}),
          jwks_uri: `${origin}/keys`,
        });
      if (pathname === '/keys') return Response.json({ keys: [publicKey] });
      if (pathname !== (external ? '/customer/token' : '/token'))
        return new Response(null, { status: 404 });
      if (!external) exchanges++;
      if (external && pauseExchange) await pauseExchange();
      const form = new URLSearchParams(await request.text());
      const code = form.get('code') ?? '';
      const grant = codes.get(code);
      codes.delete(code);
      if (
        !grant ||
        form.get('client_id') !== clientId ||
        form.get('redirect_uri') !== `https://app.example/login/oauth2/code/${registrationId}` ||
        createHash('sha256')
          .update(form.get('code_verifier') ?? '')
          .digest('base64url') !== grant.challenge
      )
        return Response.json({ error: 'invalid_grant' }, { status: 400 });
      const now = Math.floor(Date.now() / 1000);
      const payload = [
        { alg: 'RS256', kid: 'provider' },
        {
          iss: issuer,
          aud: clientId,
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
        const providerIdToken = issuedIdToken;
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
                issuerUrl: `${server.url.origin}/customer`,
                walletUrl: 'https://customer-wallet.example',
              }),
            }),
          );
        expect((await register()).status).toBe(403);
        expect((await register('wrong')).status).toBe(403);
        expect((await register(token)).status).toBe(201);
        if (scenario === 'success') {
          const links = await (await get('/login-links')).json();
          const path = '/oauth2/authorization/AppUser-another-client';
          expect(links).toContainEqual({ name: 'AppUser', url: path });
          /** Starts a fresh AppUser login and issues a code bound to its PKCE challenge. */
          async function startCustomer() {
            const start = await get(path);
            const url = new URL(start.headers.get('location') ?? '');
            expect(url.origin + url.pathname).toBe(`${server.url.origin}/customer/authorize`);
            expect(url.searchParams.get('client_id')).toBe('another-client');
            const code = crypto.randomUUID();
            codes.set(code, {
              nonce: url.searchParams.get('nonce') ?? '',
              challenge: url.searchParams.get('code_challenge') ?? '',
              wrongKey: false,
            });
            return {
              cookie: (start.headers.get('set-cookie') ?? '').split(';')[0],
              query: new URLSearchParams({ code, state: url.searchParams.get('state') ?? '' }),
            };
          }
          // A customer attempt cannot complete on the provider's callback.
          const mixed = await startCustomer();
          expect(
            (await get(`/login/oauth2/code/AppProvider?${mixed.query}`, mixed.cookie)).headers.get(
              'location',
            ),
          ).toBe('/login?error');
          const customer = await startCustomer();
          const done = await get(
            `/login/oauth2/code/AppUser-another-client?${customer.query}`,
            customer.cookie,
          );
          expect(done.headers.get('location')).toBe('/');
          const browserCookies = done.headers
            .getSetCookie()
            .map((h) => h.split(';')[0])
            .join('; ');
          expect(await (await get('/user', browserCookies)).json()).toEqual({
            name: 'Alice',
            party: 'customer',
            walletUrl: 'https://customer-wallet.example',
            isAdmin: false,
            roles: ['AppUser', 'customer'],
          });
          expect((await get('/admin/tenant-registrations', browserCookies)).status).toBe(403);
          const customerCsrf = done.headers
            .getSetCookie()
            .find((h) => h.startsWith('XSRF-TOKEN='))
            ?.split(';')[0]
            .slice('XSRF-TOKEN='.length);
          /** Returns the response to a customer callback with its original attempt cookie. */
          function finish(attempt: Awaited<ReturnType<typeof startCustomer>>) {
            return get(
              `/login/oauth2/code/AppUser-another-client?${attempt.query}`,
              attempt.cookie,
            );
          }
          /** Sends the browser's delete request with its own session and optional CSRF header. */
          function remove(cookie: string, csrf?: string) {
            return app.handle(
              new Request('http://internal/admin/tenant-registrations/AppUser', {
                method: 'DELETE',
                headers: { cookie, ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {}) },
              }),
            );
          }
          expect((await remove(browserCookies, customerCsrf)).status).toBe(403);
          expect((await remove(cookies)).status).toBe(403);
          expect((await remove(cookies, 'wrong')).status).toBe(403);
          const second = await finish(await startCustomer());
          expect(second.headers.get('location')).toBe('/');
          const secondCookie =
            second.headers
              .getSetCookie()
              .find((h) => h.startsWith('JSESSIONID='))
              ?.split(';')[0] ?? '';
          expect((await get('/user', secondCookie)).status).toBe(200);
          const pending = await startCustomer();
          const started = Promise.withResolvers<void>();
          const resume = Promise.withResolvers<void>();
          pauseExchange = async () => {
            started.resolve();
            await resume.promise;
          };
          const inFlight = finish(await startCustomer());
          try {
            await started.promise;
            expect((await remove(cookies, token)).status).toBe(204);
            expect(await (await get('/login-links')).json()).toEqual([
              { name: 'AppProvider', url: '/oauth2/authorization/AppProvider' },
            ]);
            expect((await get(path)).status).toBe(404);
            expect((await register(token)).status).toBe(201);
          } finally {
            pauseExchange = undefined;
            resume.resolve();
          }
          // A new registration with the same ID must not revive either callback or session.
          for (const rejected of [await inFlight, await finish(pending)]) {
            expect(rejected.headers.get('location')).toBe('/login?error');
            expect(rejected.headers.getSetCookie().some((h) => h.startsWith('JSESSIONID='))).toBe(
              false,
            );
          }
          for (const stale of [browserCookies, secondCookie])
            expect((await get('/user', stale)).status).toBe(401);
          const fresh = await finish(await startCustomer());
          expect(fresh.headers.get('location')).toBe('/');
          const freshCookies = fresh.headers
            .getSetCookie()
            .map((h) => h.split(';')[0])
            .join('; ');
          expect((await get('/user', freshCookies)).status).toBe(200);
          const freshCsrf = fresh.headers
            .getSetCookie()
            .find((h) => h.startsWith('XSRF-TOKEN='))
            ?.split(';')[0]
            .slice('XSRF-TOKEN='.length);
          const ended = await logout(freshCookies, freshCsrf);
          expect(new URL(ended.headers.get('location') ?? '').pathname).toBe(
            '/customer/end-session',
          );
          expect((await get('/user', freshCookies)).status).toBe(401);
        }

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
          expect(target.searchParams.get('id_token_hint')).toBe(providerIdToken);
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
