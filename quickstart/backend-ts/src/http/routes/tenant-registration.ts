import { Elysia, t } from 'elysia';
import type { resolveSharedSecretUser, Tenant } from '../../auth/shared-secret-user';

function problem(status: number, message: string) {
  return Response.json(
    { message },
    { status, headers: { 'content-type': 'application/problem+json' } },
  );
}

export function tenantRegistrationRoutes(
  tenants: Tenant[],
  sessionUser: (id: unknown) => ReturnType<typeof resolveSharedSecretUser>,
  revokeTenantSessions: (tenant: Tenant) => void,
) {
  return new Elysia({ name: 'shared-secret-tenant-registration' })
    .guard({
      transform: ({ cookie: { JSESSIONID }, set }) => {
        set.headers['cache-control'] = 'no-store';
        const user = sessionUser(JSESSIONID.value);
        if (!user) throw problem(401, 'Unauthorized');
        if (!user.isAdmin) throw problem(403, 'Forbidden');
      },
      error: ({ error, code }) => {
        if (error instanceof Response) return error;
        if (code === 'VALIDATION' || code === 'PARSE') return problem(400, 'Invalid request');
      },
    })
    .get(
      '/admin/tenant-registrations',
      () =>
        tenants.map(({ tenantId, partyId, users, internal, walletUrl }) => ({
          tenantId,
          partyId,
          users,
          internal,
          walletUrl: walletUrl ?? null,
          clientId: null,
          issuerUrl: null,
        })),
      { detail: { operationId: 'listTenantRegistrations', tags: ['Admin'] } },
    )
    .delete(
      '/admin/tenant-registrations/:tenantId',
      ({ params: { tenantId } }) => {
        const index = tenants.findIndex((tenant) => tenant.tenantId === tenantId);
        if (index === -1) return problem(404, `No tenant found for tenantId = ${tenantId}.`);
        const tenant = tenants[index];
        if (tenant.internal) return problem(403, 'Cannot delete an internal tenant');
        revokeTenantSessions(tenant);
        tenants.splice(index, 1);
        return new Response(null, { status: 204 });
      },
      { detail: { operationId: 'deleteTenantRegistration', tags: ['Admin'] } },
    )
    .post(
      '/admin/tenant-registrations',
      ({ body }) => {
        const { tenantId, partyId, users, walletUrl, issuerUrl, clientId } = body;
        if (!tenantId?.trim()) return problem(400, 'Tenant Id is required');
        if (!partyId?.trim()) return problem(400, 'Party Id is required');
        if (!users?.length)
          return problem(400, 'At least one User is required in shared-secret mode');
        if (tenants.some((tenant) => tenant.tenantId === tenantId))
          return problem(409, 'TenantId already exists');

        const names = new Set(
          tenants.flatMap((tenant) => tenant.users.map((name) => name.toLowerCase())),
        );
        for (const user of users) {
          if (user !== user.trim())
            return problem(400, 'Usernames must not have leading or trailing whitespace');
          if (!user || names.has(user.toLowerCase())) {
            console.error('Tenant registration failed: empty or duplicate local username');
            return problem(500, 'Internal Server Error');
          }
          names.add(user.toLowerCase());
        }
        for (const uri of [issuerUrl, walletUrl]) {
          if (
            uri?.trim() &&
            (/\s|[<>"{}|\\^`]|%(?![0-9a-f]{2})/i.test(uri) ||
              !URL.canParse(uri, 'http://localhost'))
          ) {
            console.error('Tenant registration failed: invalid URI');
            return problem(500, 'Internal Server Error');
          }
        }
        tenants.push({
          tenantId,
          partyId,
          users,
          internal: false,
          walletUrl: walletUrl ?? undefined,
        });
        return Response.json(
          {
            tenantId,
            partyId,
            users,
            internal: false,
            walletUrl: walletUrl?.trim() ? walletUrl : null,
            clientId: clientId ?? null,
            issuerUrl: issuerUrl?.trim() ? issuerUrl : null,
          },
          { status: 201 },
        );
      },
      {
        body: t.Object({
          tenantId: t.Optional(t.String()),
          partyId: t.Optional(t.String()),
          users: t.Optional(t.Array(t.String())),
          walletUrl: t.Optional(t.Nullable(t.String())),
          issuerUrl: t.Optional(t.Nullable(t.String())),
          clientId: t.Optional(t.Nullable(t.String())),
        }),
        detail: { operationId: 'createTenantRegistration', tags: ['Admin'] },
      },
    );
}
