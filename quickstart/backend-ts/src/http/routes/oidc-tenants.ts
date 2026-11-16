import { Elysia, t } from 'elysia';
import type { Configuration } from 'openid-client';
import type { ApplicationConfig } from '../../config/application';

/** Returns the functional error message used by the administrative API. */
function problem(status: number, message: string) {
  return Response.json(
    { message },
    { status, headers: { 'content-type': 'application/problem+json' } },
  );
}

/** Lists configured OIDC clients and publishes new registrations after successful discovery. */
export function oidcTenantRoutes(
  provider: ApplicationConfig['appProvider'],
  options: NonNullable<ApplicationConfig['oidc']>,
  discover: (issuer: URL, clientId: string) => Promise<Configuration>,
  authorize: (
    request: Request,
    sessionId: unknown,
    csrfCookie: unknown,
  ) => Promise<200 | 401 | 403>,
) {
  const registrations = new Map([
    [
      provider.tenantId as string,
      {
        tenantId: provider.tenantId as string,
        partyId: provider.partyId,
        internal: true,
        walletUrl: null as string | null,
        users: null,
        clientId: options.clientId,
        issuerUrl: options.issuer.href,
      },
    ],
  ]);
  /** Match Java's unique tenant ID, issuer/client pair, and generated registration ID. */
  function conflict(tenantId: string, clientId: string, issuerUrl: string) {
    if (registrations.has(tenantId)) return problem(409, 'TenantId already exists');
    for (const entry of registrations.values()) {
      if (entry.clientId === clientId && entry.issuerUrl === issuerUrl)
        return problem(409, 'ClientId-IssuerUrl combination already exists');
      if (!entry.internal && `${entry.tenantId}-${entry.clientId}` === `${tenantId}-${clientId}`)
        return problem(409, 'RegistrationId already exists');
    }
  }
  return new Elysia({ name: 'oidc-tenants', prefix: '/admin/tenant-registrations' })
    .onTransform(async ({ request, cookie, set }) => {
      set.headers['cache-control'] = 'no-store';
      const access = await authorize(request, cookie.JSESSIONID.value, cookie['XSRF-TOKEN'].value);
      if (access !== 200) throw problem(access, access === 401 ? 'Unauthorized' : 'Forbidden');
    })
    .onError(({ error, code }) => {
      if (error instanceof Response) return error;
      if (code === 'VALIDATION' || code === 'PARSE') return problem(400, 'Invalid request');
      console.error(
        'OIDC tenant administration failed',
        error instanceof Error ? error.name : 'unknown error',
      );
      return problem(500, 'Internal Server Error');
    })
    .get('', () => Array.from(registrations.values()), {
      detail: { operationId: 'listTenantRegistrations', tags: ['Admin'] },
    })
    .post(
      '',
      async ({ body, status }) => {
        const { tenantId, partyId, clientId, issuerUrl, walletUrl, users } = body;
        if (!tenantId?.trim()) return problem(400, 'Tenant Id is required');
        if (!partyId?.trim()) return problem(400, 'Party Id is required');
        if (!clientId?.trim()) return problem(400, 'Client Id is required in OAuth2 mode');
        if (!issuerUrl?.trim()) return problem(400, 'Issuer Url is required in OAuth2 mode');
        const issuer = URL.parse(issuerUrl);
        if (
          !issuer ||
          issuer.username ||
          issuer.password ||
          issuer.search ||
          issuer.hash ||
          !(issuer.protocol === 'https:' || (options.allowHttp && issuer.protocol === 'http:'))
        )
          return problem(400, 'Invalid issuer URL');
        if (
          walletUrl?.trim() &&
          (!URL.canParse(walletUrl, 'https://localhost') || /\s|%(?![\da-f]{2})/i.test(walletUrl))
        )
          return problem(400, 'Invalid wallet URL');
        const duplicate = conflict(tenantId, clientId, issuer.href);
        if (duplicate) return duplicate;
        const config = await discover(issuer, clientId);
        if (
          !config.serverMetadata().authorization_endpoint ||
          !config.serverMetadata().token_endpoint ||
          !config.serverMetadata().jwks_uri
        )
          throw new Error('Incomplete OIDC discovery metadata');
        // Another request may have registered the same tenant while discovery was in flight.
        const raced = conflict(tenantId, clientId, issuer.href);
        if (raced) return raced;
        const entry = {
          tenantId,
          partyId,
          clientId,
          issuerUrl: issuer.href,
          walletUrl: walletUrl?.trim() ? walletUrl : null,
          internal: false,
          users: null,
        };
        registrations.set(tenantId, entry);
        return status(201, { ...entry, users: users ?? null });
      },
      {
        body: t.Object({
          tenantId: t.Optional(t.String()),
          partyId: t.Optional(t.String()),
          clientId: t.Optional(t.String()),
          issuerUrl: t.Optional(t.String()),
          walletUrl: t.Optional(t.Nullable(t.String())),
          users: t.Optional(t.Nullable(t.Array(t.String()))),
        }),
        detail: { operationId: 'createTenantRegistration', tags: ['Admin'] },
      },
    );
}
