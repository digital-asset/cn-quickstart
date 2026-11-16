import { expect, test } from 'bun:test';
import { createApp } from '../src/app';
import { appDependencies } from './fixtures/dependencies';

test('Login opens a session and logout revokes the original cookie', async () => {
  const app = createApp(appDependencies);
  const login = await app.handle(
    new Request('http://localhost/login', {
      method: 'POST',
      body: new URLSearchParams({ username: 'app-provider' }),
    }),
  );
  expect(login.status).toBe(302);
  expect(login.headers.get('location')).toBe('/');
  const header = login.headers.get('set-cookie') ?? '';
  expect(header).toMatch(/^JSESSIONID=[^;]+;/);
  expect(header).toContain('HttpOnly');
  const headers = { cookie: header.split(';')[0] };
  const user = () => app.handle(new Request('http://localhost/user', { headers }));
  const response = await user();
  expect(response.status).toBe(200);
  expect(response.headers.get('cache-control')).toBe('no-store');
  expect(await response.json()).toEqual({
    name: 'app-provider the provider',
    party: 'test-provider',
    roles: ['ROLE_ADMIN'],
    isAdmin: true,
    walletUrl: null,
  });
  const logout = await app.handle(
    new Request('http://localhost/logout', { method: 'POST', headers }),
  );
  expect(logout.status).toBe(302);
  expect(logout.headers.get('location')).toBe('/');
  expect(logout.headers.get('set-cookie')).toContain('Max-Age=0');
  expect((await user()).status).toBe(401);
});

test('Unknown users and incorrect passwords cannot open a session', async () => {
  const app = createApp(appDependencies);
  expect((await app.handle(new Request('http://localhost/user'))).status).toBe(401);
  for (const [username, password] of [
    ['someone', ''],
    ['app-provider', 'wrong'],
  ]) {
    const result = await app.handle(
      new Request('http://localhost/login', {
        method: 'POST',
        body: new URLSearchParams({ username, password }),
      }),
    );
    expect(result.status).toBe(302);
    expect(result.headers.get('location')).toBe('/login?error');
    expect(result.headers.has('set-cookie')).toBe(false);
  }
});
