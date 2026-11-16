import { expect, test } from 'bun:test';
import { createApp } from '../src/app';
import { appDependencies } from './fixtures/dependencies';

function setup() {
  const app = createApp(appDependencies);
  const login = async (username: string) => {
    const response = await app.handle(
      new Request('http://localhost/login', {
        method: 'POST',
        body: new URLSearchParams({ username }),
      }),
    );
    return response.headers.get('set-cookie')?.split(';')[0] ?? '';
  };
  const register = (cookie: string, data: object) =>
    app.handle(
      new Request('http://localhost/admin/tenant-registrations', {
        method: 'POST',
        headers: { cookie, 'content-type': 'application/json', authorization: 'Custom' },
        body: JSON.stringify(data),
      }),
    );
  const list = (cookie: string) =>
    app.handle(new Request('http://localhost/admin/tenant-registrations', { headers: { cookie } }));
  return { app, login, register, list };
}

test('Only admins can onboard a tenant and failed registration reserves no users or tenant ID', async () => {
  const { app, login, register } = setup();
  const tenant = {
    tenantId: 'AppUser',
    partyId: 'customer::123',
    walletUrl: 'http://wallet.localhost:2000',
    issuerUrl: '',
    clientId: '',
    internal: true,
    users: ['alice'],
  };

  // The onboarding script sends Authorization: Custom, but it needs an admin session cookie.
  expect((await register('', tenant)).status).toBe(401);
  const admin = await login('app-provider');
  const missing = await register(admin, { ...tenant, users: [] });
  expect(missing.status).toBe(400);
  expect(await missing.json()).toEqual({
    message: 'At least one User is required in shared-secret mode',
  });

  // The second username collides with app-provider. Both alice and the tenant ID must remain available for retry.
  expect((await register(admin, { ...tenant, users: ['alice', 'APP-PROVIDER'] })).status).toBe(500);
  expect(await login('alice')).toBe('');
  const created = await register(admin, tenant);
  expect(created.status).toBe(201);
  expect(await created.json()).toEqual({ ...tenant, internal: false, issuerUrl: null });
  expect((await register(admin, tenant)).status).toBe(409);

  // Even though the request asked for internal: true, alice should only have regular user access.
  const cookie = await login('alice');
  const user = await app.handle(new Request('http://localhost/user', { headers: { cookie } }));
  expect(await user.json()).toEqual({
    name: 'alice the user',
    party: tenant.partyId,
    walletUrl: tenant.walletUrl,
    roles: ['ROLE_USER'],
    isAdmin: false,
  });
  expect((await register(cookie, { ...tenant, tenantId: 'other' })).status).toBe(403);
});

test('Only admins can list the configured provider and registered tenants', async () => {
  const { login, register, list } = setup();
  expect((await list('')).status).toBe(401);
  const admin = await login('app-provider');
  const provider = {
    tenantId: 'AppProvider',
    partyId: 'test-provider',
    internal: true,
    users: ['app-provider'],
    walletUrl: null,
    issuerUrl: null,
    clientId: null,
  };

  // Only the configured provider exists before onboarding.
  const initial = await list(admin);
  expect(initial.status).toBe(200);
  expect(await initial.json()).toEqual([provider]);

  const tenant = {
    tenantId: 'AppUser',
    partyId: 'customer::123',
    walletUrl: 'http://wallet.localhost:2000',
    users: ['alice', 'bob'],
  };
  expect((await register(admin, tenant)).status).toBe(201);
  const response = await list(admin);
  expect(response.status).toBe(200);
  const registrations = await response.json();
  expect(registrations).toHaveLength(2);
  expect(registrations).toContainEqual(provider);
  expect(registrations).toContainEqual({
    ...tenant,
    internal: false,
    issuerUrl: null,
    clientId: null,
  });

  // A registered user can sign in but cannot see the tenant directory.
  expect((await list(await login('alice'))).status).toBe(403);
});

test('Deleting an external tenant revokes every session even after re-registration', async () => {
  const { app, login, register, list } = setup();
  const remove = (cookie: string, id: string) =>
    app.handle(
      new Request(`http://localhost/admin/tenant-registrations/${id}`, {
        method: 'DELETE',
        headers: { cookie },
      }),
    );
  const user = (cookie: string) =>
    app.handle(new Request('http://localhost/user', { headers: { cookie } }));
  const tenant = { tenantId: 'AppUser', partyId: 'customer::123', users: ['Alice', 'bob'] };
  const admin = await login('app-provider');
  expect((await register(admin, tenant)).status).toBe(201);
  const sessions = [await login('ALICE'), await login('alice'), await login('bob')];
  expect(new Set(sessions).size).toBe(3);
  expect(await (await user(sessions[0])).json()).toMatchObject({ name: 'Alice the user' });

  // Rejected deletes must leave both the provider and the external users active.
  expect((await remove('', 'AppUser')).status).toBe(401);
  expect((await remove(sessions[0], 'AppUser')).status).toBe(403);
  expect((await remove(admin, 'AppProvider')).status).toBe(403);
  for (const cookie of sessions) expect((await user(cookie)).status).toBe(200);
  expect(await login('app-provider')).not.toBe('');

  const deleted = await remove(admin, 'AppUser');
  expect(deleted.status).toBe(204);
  expect(await deleted.text()).toBe('');
  const missing = await remove(admin, 'AppUser');
  expect(missing.status).toBe(404);
  expect(await missing.json()).toEqual({ message: 'No tenant found for tenantId = AppUser.' });
  expect(await (await list(admin)).json()).toEqual([
    expect.objectContaining({ tenantId: 'AppProvider' }),
  ]);
  expect(await login('alice')).toBe('');
  for (const cookie of sessions) expect((await user(cookie)).status).toBe(401);

  // Reusing the same tenant ID and usernames requires a fresh login.
  expect((await register(admin, tenant)).status).toBe(201);
  for (const cookie of sessions) expect((await user(cookie)).status).toBe(401);
  expect((await user(await login('alice'))).status).toBe(200);
});
