// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import { test, expect, BrowserContext, Page } from '@playwright/test';
// @ts-ignore
import { PROVIDER_STORAGE } from './global.ts';
const APP_PROVIDER_BASE = 'http://app-provider.localhost:3000';
const ADMIN_TENANT_REG_URL = `${APP_PROVIDER_BASE}/tenants`;
const FEATURE_FLAGS_URL = `${APP_PROVIDER_BASE}/api/feature-flags`;
const OIDC_ISSUER = 'http://keycloak.localhost:8082/realms/AppProvider';

async function fetchAuthMode(page: Page): Promise<'oauth2' | 'shared-secret'> {
    const res = await page.request.get(FEATURE_FLAGS_URL);
    expect(res.ok()).toBeTruthy();
    const json = await res.json();
    return json.authMode as 'oauth2' | 'shared-secret';
}

async function goToTenantRegistrations(page: Page) {
    await page.goto(ADMIN_TENANT_REG_URL);
    // Wait for the table or form to be present
    await expect(page.getByRole('heading', { name: /Existing Tenant Registrations/i })).toBeVisible({ timeout: 10000 });
}

async function fillCommon(page: Page, { tenantId, partyId, walletUrl }: { tenantId: string; partyId: string; walletUrl: string; }) {
    await page.getByLabel('Tenant ID:').fill(tenantId);
    await page.getByLabel('PartyId:').fill(partyId);
    await page.getByLabel('Wallet URL:').fill(walletUrl);
}

async function fillOAuth2(page: Page, { clientId, issuerUrl }: { clientId: string; issuerUrl: string; }) {
    await page.getByLabel('Client ID:').fill(clientId);
    await page.getByLabel('Issuer URL:').fill(issuerUrl);
}

async function fillSharedSecret(page: Page, { users }: { users: string }) {
    await page.getByLabel('Users (comma-separated):').fill(users);
}

test.describe('Tenant Registrations (E2E)', () => {
    test.describe.configure({ mode: 'serial' });

    let providerContext: BrowserContext;
    let providerPage: Page;

    test.beforeAll(async ({ browser }) => {
        // Start from a logged-in provider session if you persisted it before,
        // otherwise this context should already be authenticated by your prior suite.
        providerContext = await browser.newContext({ storageState: PROVIDER_STORAGE });
        providerPage = await providerContext.newPage();
    });

    test.beforeEach(async () => {
        await providerPage.addInitScript(() => {
            // @ts-ignore
            // this disables HTML5 form validation so we can test backend validation
            window.__E2E_NOVALIDATE__ = true;
            // @ts-ignore
            window.alert = () => {};
            // @ts-ignore
            window.prompt = () => null;
            // @ts-ignore
            window.confirm = () => true;
        });
    });

    test.afterAll(async () => {
        await providerContext?.close();
    });

    test('Create tenant (happy path) -> success toast & row appears', async () => {
        const mode = await fetchAuthMode(providerPage);
        await goToTenantRegistrations(providerPage);

        const tenantId = `e2e-tenant-${Date.now()}`;
        const partyId = `party-${Math.floor(Math.random() * 1e6)}`;
        const walletUrl = 'http://wallet.localhost:2000/';

        await fillCommon(providerPage, { tenantId, partyId, walletUrl });

        if (mode === 'oauth2') {
            await fillOAuth2(providerPage, {
                clientId: `client-${Date.now()}`,
                issuerUrl: OIDC_ISSUER,
            });
        } else {
            await fillSharedSecret(providerPage, { users: 'alice, bob' });
        }

        const submit = providerPage.getByRole('button', { name: /Submit/i });
        await expect(submit).toBeEnabled();

        await submit.click();
        await expect(providerPage.getByText(/Tenant registration created/i)).toBeVisible();
        await expect(providerPage.getByRole('row', { name: new RegExp(tenantId) })).toBeVisible();

        await expect(submit).toBeEnabled();
    });

    test('400 shows backend reason (missing required field for mode)', async () => {
        const mode = await fetchAuthMode(providerPage);
        await goToTenantRegistrations(providerPage);

        const tenantId = `e2e-tenant-400-${Date.now()}`;
        const partyId = `party-${Math.floor(Math.random() * 1e6)}`;
        const walletUrl = 'http://wallet.localhost:2000/';

        await fillCommon(providerPage, { tenantId, partyId, walletUrl });

        if (mode === 'oauth2') {
            // Omit issuerUrl (or clientId) so server returns 400 with message
            await fillOAuth2(providerPage, { clientId: `c-${Date.now()}`, issuerUrl: '' });
        } else {
            await fillSharedSecret(providerPage, { users: 'alice, bob' });
        }
        const submit = providerPage.getByRole('button', { name: /Submit/i });
        await expect(submit).toBeEnabled();
        await submit.click();

        await expect(providerPage.getByText(/issuerUrl is required in OAuth2 mode/i)).toBeVisible();
        await expect(submit).toBeEnabled(); // returns to enabled after request completes
    });

    test('409 conflict on duplicate tenantId -> shows reason + delete confirmation flow', async () => {
        const mode = await fetchAuthMode(providerPage);
        await goToTenantRegistrations(providerPage);

        const tenantId = `e2e-tenant-dup-${Date.now()}`;
        const partyId = `party-${Math.floor(Math.random() * 1e6)}`;
        const walletUrl = 'http://wallet.localhost:2000/';

        // First creation (should succeed)
        await fillCommon(providerPage, { tenantId, partyId, walletUrl });
        if (mode === 'oauth2') {
            await fillOAuth2(providerPage, {
                clientId: `client-${Date.now()}`,
                issuerUrl: OIDC_ISSUER,
            });
        } else {
            await fillSharedSecret(providerPage, { users: 'carol' });
        }
        await providerPage.getByRole('button', { name: /submit/i }).click();
        await expect(providerPage.getByText(/Tenant registration created/i)).toBeVisible();
        await expect(providerPage.getByRole('row', { name: new RegExp(tenantId) })).toBeVisible();

        // Second creation with the same tenantId (should trigger 409)
        await fillCommon(providerPage, { tenantId, partyId: `party-${Math.floor(Math.random() * 1e6)}`, walletUrl });
        if (mode === 'oauth2') {
            await fillOAuth2(providerPage, {
                clientId: `client-${Date.now() + 1}`,
                issuerUrl: `http://issuer-${Date.now() + 1}.example`,
            });
        } else {
            await fillSharedSecret(providerPage, { users: 'dave' });
        }
        await providerPage.getByRole('button', { name: /submit/i }).click();

        await expect(providerPage.getByText(/TenantId already exists/i)).toBeVisible();

        // Cleanup: delete the first-created
        // Find the row for tenantId and click its Delete button (disabled if internal)
        const row = providerPage.getByRole('row', { name: new RegExp(tenantId) });
        const delBtn = row.getByRole('button', { name: /delete/i });
        await expect(delBtn).toBeVisible();
        await expect(delBtn).toBeEnabled();
        await delBtn.scrollIntoViewIfNeeded();
        await delBtn.click();

        await expect(row).toHaveCount(0, { timeout: 20_000 });
        await expect(providerPage.getByText(/Tenant registration deleted/i)).toBeVisible();
    });
});