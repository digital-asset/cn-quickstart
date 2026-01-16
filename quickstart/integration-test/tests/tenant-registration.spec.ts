// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import { test } from '../fixtures/workflow';
// @ts-ignore
import { WALLET_URL } from './global.ts';
import { Button as TenantButton } from "../pages/sections/tenants.tab";


import { expect, BrowserContext, Page } from '@playwright/test';


test.describe('Tenant Registrations (E2E)', () => {
    test.beforeEach(async ({ provider }) => {
        await provider.tenants.stubConfirmAlwaysTrue();
    });

    test('Create tenant (happy path) -> success toast & row appears', async ({ provider }) => {
        const tenantId = `e2e-tenant-${Date.now()}`;
        const partyId = `party-${Math.floor(Math.random() * 1e6)}`;
        const walletUrl = WALLET_URL;

        await provider.tenants.goto();
        await provider.tenants.fillCommon({ tenantId, partyId, walletUrl });
        await provider.tenants.fillSecuritySection({});
        await provider.tenants.clickSubmit();
        await provider.waitForSuccessMessage('Tenant registration created');
        await provider.tenants.withRowMatching(tenantId, async () => {
            await provider.tenants.assertMatchingRowCountIs(1);
        });
        await provider.tenants.assertSubmitEnabled();
    });

    test('409 conflict, ClientId-IssuerUrl combination should be unique -> shows reason', async ({ provider }) => {
        const tenantId = `e2e-tenant-400-${Date.now()}`;
        const partyId = `party-${Math.floor(Math.random() * 1e6)}`;
        const walletUrl = 'http://wallet.localhost:2000/';
        const clientId = `client-${Date.now()}`;

        await provider.tenants.goto();
        // First creation (should succeed)
        await provider.tenants.fillCommon({ tenantId, partyId, walletUrl });
        await provider.tenants.fillSecuritySection({ clientId: clientId });
        await provider.tenants.clickSubmit();
        await provider.waitForSuccessMessage('Tenant registration created');
        await provider.tenants.withRowMatching(tenantId, async () => {
            await provider.tenants.assertMatchingRowCountIs(1);
        });
        // Second creation with the same ClientId-IssuerUrl combination (should trigger 409)
        const tenantId2 = `e2e-tenant-400-2-${Date.now()}`;
        const partyId2 = `party-2-${Math.floor(Math.random() * 1e6)}`;

        await provider.tenants.fillCommon({ tenantId: tenantId2, partyId: partyId2, walletUrl });
        await provider.tenants.fillSecuritySection({ clientId: clientId });
        await provider.tenants.clickSubmit();
        await provider.tenants.assertErrorVisible(/ClientId-IssuerUrl combination already exists/i);
        // Cleanup: delete the first-created
        // Find the row for tenantId and click its Delete button (disabled if internal)
        await provider.tenants.withRowMatching(tenantId, async () => {
            await provider.tenants.clickButton(TenantButton.Delete);
            await provider.waitForSuccessMessage('Tenant registration deleted');
            await provider.tenants.assertNoMatchingRowExists();
        });
    });

    test('409 conflict on duplicate tenantId -> shows reason + delete confirmation flow', async ({ provider }) => {
        const mode = await provider.tenants.getAuthMode();
        const tenantId = `e2e-tenant-dup-${Date.now()}`;
        const partyId = `party-${Math.floor(Math.random() * 1e6)}`;
        const walletUrl = WALLET_URL;

        await provider.tenants.goto();
        // First creation (should succeed)
        await provider.tenants.fillCommon({ tenantId, partyId, walletUrl });
        await provider.tenants.fillSecuritySection({});
        await provider.tenants.clickSubmit();
        await provider.waitForSuccessMessage('Tenant registration created');
        await provider.tenants.withRowMatching(tenantId, async () => {
            await provider.tenants.assertMatchingRowCountIs(1);
        });
        // Second creation with the same tenantId (should trigger 409)
        await provider.tenants.fillCommon({ tenantId, partyId: `party-${Math.floor(Math.random() * 1e6)}`, walletUrl });
        await provider.tenants.fillSecuritySection({});
        await provider.tenants.clickSubmit();
        await provider.tenants.assertErrorVisible(/TenantId already exists/i);
        // Cleanup: delete the first-created
        // Find the row for tenantId and click its Delete button (disabled if internal)
        await provider.tenants.withRowMatching(tenantId, async () => {
            await provider.tenants.clickButton(TenantButton.Delete);
            await provider.waitForSuccessMessage('Tenant registration deleted');
            await provider.tenants.assertNoMatchingRowExists();
        });
    });
});
