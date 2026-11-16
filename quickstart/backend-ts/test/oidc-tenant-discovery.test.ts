import { expect, test } from 'bun:test';
import * as oidc from 'openid-client';
import { initialRegistrations, oidcTenantRoutes } from '../src/http/routes/oidc-tenants';

const endpoint = 'http://localhost/admin/tenant-registrations';

/** Exercises registration as an authorized admin against a local discovery server. */
function setup(allowHttp: boolean) {
  let requests = 0;
  let publishKeys = false;
  const server = Bun.serve({
    hostname: '127.0.0.1',
    port: 0,
    /** Serves valid discovery metadata except for the initially missing JWKS URI. */
    fetch(request) {
      requests++;
      const url = new URL(request.url);
      if (url.pathname !== '/tenant/.well-known/openid-configuration')
        return new Response(null, { status: 404 });
      return Response.json({
        issuer: `${url.origin}/tenant`,
        authorization_endpoint: `${url.origin}/authorize`,
        token_endpoint: `${url.origin}/token`,
        ...(publishKeys ? { jwks_uri: `${url.origin}/keys` } : {}),
      });
    },
  });
  const registrations = initialRegistrations(
    { tenantId: 'AppProvider', partyId: 'provider', internal: true },
    {
      issuer: new URL('https://provider.example'),
      clientId: 'provider-client',
      publicOrigin: 'https://app.example',
      testProfile: false,
      allowHttp,
    },
  );
  const app = oidcTenantRoutes(
    registrations,
    allowHttp,
    // Let the test transport reach localhost so a missing route-level check is observable.
    (issuer, clientId) =>
      oidc.discovery(issuer, clientId, undefined, oidc.None(), {
        execute: [oidc.allowInsecureRequests],
      }),
    async () => 200,
  );
  const payload = {
    tenantId: 'AppUser',
    partyId: 'user',
    clientId: 'user-client',
    issuerUrl: `${server.url.origin}/tenant`,
  };
  return {
    server,
    get requests() {
      return requests;
    },
    /** Repairs the provider metadata without changing the registration request. */
    publishKeys() {
      publishKeys = true;
    },
    /** Submits the same tenant on each attempt. */
    register() {
      return app.handle(
        new Request(endpoint, {
          method: 'POST',
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify(payload),
        }),
      );
    },
    /** Lists tenants visible to the administrator after each registration attempt. */
    async list() {
      const response = await app.handle(new Request(endpoint));
      expect(response.status).toBe(200);
      return response.json();
    },
  };
}

test('An HTTP issuer is rejected before discovery when HTTP is disabled', async () => {
  const fixture = setup(false);
  try {
    const response = await fixture.register();
    expect(response.status).toBe(400);
    expect(await response.json()).toEqual({ message: 'Invalid issuer URL' });
    expect(fixture.requests).toBe(0);
    expect(await fixture.list()).toEqual([expect.objectContaining({ tenantId: 'AppProvider' })]);
  } finally {
    fixture.server.stop(true);
  }
});

test('Discovery without jwks_uri leaves the tenant available for an identical retry', async () => {
  const fixture = setup(true);
  try {
    const rejected = await fixture.register();
    expect(rejected.status).toBe(500);
    expect(await rejected.json()).toEqual({ message: 'Internal Server Error' });
    expect(fixture.requests).toBe(1);
    expect(await fixture.list()).toEqual([expect.objectContaining({ tenantId: 'AppProvider' })]);

    fixture.publishKeys();
    expect((await fixture.register()).status).toBe(201);
    const tenants = await fixture.list();
    expect(tenants).toHaveLength(2);
    expect(tenants).toContainEqual(
      expect.objectContaining({ tenantId: 'AppUser', clientId: 'user-client' }),
    );
  } finally {
    fixture.server.stop(true);
  }
});
