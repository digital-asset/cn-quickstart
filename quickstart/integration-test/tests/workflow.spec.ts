// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import {test, expect, BrowserContext, Page} from '@playwright/test';
import {getVisibleLocator, createAppInstallRequests} from './helpers';

// Reused runtime values
const APP_PROVIDER_LOGIN_URL = 'http://app-provider.localhost:3000/login';
const APP_PROVIDER_APP_INSTALLS_URL = 'http://app-provider.localhost:3000/app-installs';
const APP_PROVIDER_LICENSES_URL = 'http://app-provider.localhost:3000/licenses';
const WALLET_URL = 'http://wallet.localhost:2000/';
const DEFAULT_PASSWORD = 'abc123';
const REQUEST_STATUS = 'REQUEST';
const INSTALL_STATUS = 'INSTALL';
const TEST_RENEWAL_REASON = 'test renewal reason';

test.describe('AppInstall and Licensing workflow', () => {
    test.describe.configure({mode: 'serial'});

    let providerContext: BrowserContext;
    let providerPage: Page;

    let userContext: BrowserContext;
    let userPage: Page;

    test.beforeAll(async ({browser, request}) => {
        providerContext = await browser.newContext();
        providerPage = await providerContext.newPage();

        userContext = await browser.newContext();
        userPage = await userContext.newPage();
        await createAppInstallRequests(request);
    });

    test.afterAll(async () => {
        await providerContext.close();
        await userContext.close();
    });

    test('Check initial availability of AppUser login link, ensure application ready', async () => {
        await userPage.goto(APP_PROVIDER_LOGIN_URL);
        for (let i = 0; i < 10; i++) {
            if (await userPage.locator('a:has-text("AppUser")').isVisible()) {
                return;
            }
            await new Promise((r) => setTimeout(r, 1000));
            await userPage.reload();
        }
        await expect(userPage.locator('a:has-text("AppUser")'), 'AppUser login link not visible after 10 attempts').toBeVisible();
    });

    test('AppUser can log in', async () => {
        await userPage.goto(APP_PROVIDER_LOGIN_URL);
        await (
            await getVisibleLocator(userPage, 'a:has-text("AppUser")', 'Has AppUser login link')
        ).click();
        await (
            await getVisibleLocator(userPage, 'input[id="username"]', 'Username field is visible')
        ).fill('alice');
        await (
            await getVisibleLocator(userPage, 'input[id="password"]', 'Password field is visible')
        ).fill(DEFAULT_PASSWORD);
        await (
            await getVisibleLocator(userPage, 'button[id="kc-login"]', 'Login button is visible')
        ).click();
        await expect(userPage.locator('#user-name')).toHaveText('Alice the user');
    });

    test('AppProvider can log in', async () => {
        await providerPage.goto(APP_PROVIDER_LOGIN_URL);
        await (
            await getVisibleLocator(providerPage, 'a:has-text("AppProvider")', 'Has AppProvider login link')
        ).click();
        await (
            await getVisibleLocator(providerPage, 'input[id="username"]', 'Username field is visible')
        ).fill('pat');
        await (
            await getVisibleLocator(providerPage, 'input[id="password"]', 'Password field is visible')
        ).fill(DEFAULT_PASSWORD);
        await (
            await getVisibleLocator(providerPage, 'button[id="kc-login"]', 'Login button is visible')
        ).click();
        await expect(providerPage.locator('#user-name')).toHaveText('Pat the provider');
    });

    test('AppUser can sign in to wallet UI', async () => {
        await userPage.goto(WALLET_URL);
        await (
            await getVisibleLocator(userPage, '#oidc-login-button', 'Login button is visible')
        ).click();
        await (
            await getVisibleLocator(userPage, '#password', 'Password field is visible')
        ).fill(DEFAULT_PASSWORD);
        await (
            await getVisibleLocator(userPage, '#kc-login', 'Login button is visible')
        ).click();
        await expect(userPage.locator('#logged-in-user')).toBeVisible();
    });

    test('Prerequisite: Add funds to AppUser wallet', async () => {
        await userPage.goto(WALLET_URL);
        await (
            await getVisibleLocator(userPage, '#tap-amount-field', 'Tap amount field is visible')
        ).fill('1000');
        await (
            await getVisibleLocator(userPage, '#tap-button', 'Tap button is visible')
        ).click();
        test.slow(); // CC processing can take a while
        await expect(
            userPage.locator('[data-testid="AccountBalanceWalletIcon"]').first(),
            'Balance update should show in transaction history.'
        ).toBeVisible();
    });

    test('AppProvider can see all four AppInstallRequests', async () => {
        await providerPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await expect(
            providerPage.locator('.app-install-row'),
            'Should have exactly four rows in the app installs table. Assumes that make create-app-install-request was run four times.'
        ).toHaveCount(4);
        await expect(
            providerPage.locator('td.app-install-status', {hasText: REQUEST_STATUS}),
            'There should be exactly four "REQUEST" status cells.'
        ).toHaveCount(4);
    });

    test('AppUser can see all four AppInstallRequests', async () => {
        await userPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await expect(
            userPage.locator('.app-install-row'),
            'Should have exactly four rows in the app installs table. Assumes that make create-app-install-request was run four times.'
        ).toHaveCount(4);
        await expect(
            userPage.locator('td.app-install-status', {hasText: REQUEST_STATUS}),
            'There should be exactly four "REQUEST" status cells.'
        ).toHaveCount(4);
    });

    test('AppProvider can reject an AppInstallRequest', async () => {
        await providerPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await providerPage.locator('button.btn-reject-install').first().click();
        await expect(
            providerPage.locator('.app-install-row'),
            'Should have three rows in the app installs table after rejecting one request.'
        ).toHaveCount(3);
    });

    test('AppProvider can accept an AppInstallRequests', async () => {
        await providerPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await providerPage.locator('button.btn-accept-install').first().click();
        await expect(
            providerPage.locator('td.app-install-status', {hasText: INSTALL_STATUS}),
            'Should have exactly one "INSTALL" status cell.'
        ).toHaveCount(1);

        await providerPage.locator('button.btn-accept-install').first().click();
        await expect(
            providerPage.locator('td.app-install-status', {hasText: INSTALL_STATUS}),
            'Should have exactly two "INSTALL" status cells.'
        ).toHaveCount(2);

        await providerPage.locator('button.btn-accept-install').first().click();
        await expect(
            providerPage.locator('td.app-install-status', {hasText: INSTALL_STATUS}),
            'Should have exactly three "INSTALL" status cells.'
        ).toHaveCount(3);
    });

    test('AppProvider can cancel an AppInstall', async () => {
        await userPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await userPage.locator('button.btn-cancel-install').first().click();
        await expect(
            userPage.locator('.app-install-row'),
            'Should have exactly two rows in the app installs table. Assumes that make create-app-install-request was run four times before starting this test.'
        ).toHaveCount(2);
    });

    test('AppUser can see accepted AppInstalls', async () => {
        await userPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await expect(
            userPage.locator('td.app-install-status', {hasText: INSTALL_STATUS}),
            'Should have exactly two "INSTALL" status cells.'
        ).toHaveCount(2);
    });

    test('AppUser can cancel an AppInstall', async () => {
        await userPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await userPage.locator('button.btn-cancel-install').first().click();
        await expect(
            userPage.locator('.app-install-row'),
            'Should have exactly two rows in the app installs table.'
        ).toHaveCount(2);
    });

    test('AppProvider can create licenses', async () => {
        await providerPage.goto(APP_PROVIDER_APP_INSTALLS_URL);
        await expect(
            providerPage.locator('td.app-install-status', {hasText: INSTALL_STATUS}),
            'Should have one "INSTALL" status cell.'
        ).toHaveCount(1);
        await expect(
            providerPage.locator('.app-install-num-licenses', {hasText: '0'}),
            'precondition: AppInstall should not have created any licenses yet'
        ).toHaveCount(1);

        await providerPage.locator('button.btn-create-license').first().click();
        await expect(providerPage.locator('.app-install-num-licenses', {hasText: '1'})).toHaveCount(1);

        await providerPage.locator('button.btn-create-license').first().click();
        await expect(providerPage.locator('.app-install-num-licenses', {hasText: '2'})).toHaveCount(1);

        await providerPage.goto(APP_PROVIDER_LICENSES_URL);
        await expect(providerPage.locator('.license-row')).toHaveCount(2);
    });

    test('AppUser can see created licenses', async () => {
        await userPage.goto(APP_PROVIDER_LICENSES_URL);
        await expect(userPage.locator('.license-row')).toHaveCount(2);
    });

    test('AppProvider can expire a license', async () => {
        await providerPage.goto(APP_PROVIDER_LICENSES_URL);
        await expect(providerPage.locator('.license-row')).toHaveCount(2);

        await providerPage.locator('.license-row .btn-actions-license').first().click();
        await providerPage.locator('input.input-expire-description').fill('Testing license expiration');
        await providerPage.locator('button.btn-expire-license').click();

        await expect(providerPage.locator('.license-row')).toHaveCount(1);
    });

    test('AppUser sees no license action buttons on non-renewable license', async () => {
        await userPage.goto(APP_PROVIDER_LICENSES_URL);
        // Checking from user perspective
        await expect(providerPage.locator('.license-row')).toHaveCount(1);
        await expect(userPage.locator('.license-actions button')).toHaveCount(0);
        await expect(userPage.locator('.license-actions a')).toHaveCount(0);
    });

    test('AppProvider can issue a license renewal', async () => {
        await providerPage.goto(APP_PROVIDER_LICENSES_URL);
        await expect(providerPage.locator('.license-row')).toHaveCount(1);

        await providerPage.locator('.btn-actions-license').first().click();
        await providerPage.locator('input.input-renew-description').fill(TEST_RENEWAL_REASON);
        await providerPage.locator('button.btn-issue-renewal').click();

        await expect(
            providerPage.locator('td.license-renew-fee', {hasText: '100'})
        ).toHaveCount(1);
        await expect(
            providerPage.locator('td.license-extension', {hasText: '30 days'})
        ).toHaveCount(1);
    });

    test('AppUser can pay License Renewal through wallet workflow', async () => {
        await userPage.goto(APP_PROVIDER_LICENSES_URL);
        await (
            await getVisibleLocator(userPage, '.btn-pay-renewal', 'License payment button visible')
        ).click();
        await userPage.waitForURL(/.*wallet.localhost.*/);
        await expect(
            userPage.locator('.payment-description', {hasText: TEST_RENEWAL_REASON}),
            'Matching license renewal reason visible'
        ).toBeVisible();
        await (
            await getVisibleLocator(userPage, '.payment-accept', 'Send payment button visible')
        ).click();
        await userPage.waitForURL(/.*app-provider.localhost.*/);
    });

    test('AppProvider can complete renewal on a paid-for license', async () => {
        await providerPage.goto(APP_PROVIDER_LICENSES_URL);
        const completeRenewalBtn = providerPage.locator('.btn-complete-renewal');
        test.slow(); // payment processing can take a while
        await expect(completeRenewalBtn, 'Paid license is renewable').toHaveCount(1, {timeout: 30_000});
        await completeRenewalBtn.click();
        await expect(
            completeRenewalBtn,
            'Complete Renewal button disappears after payment acceptance',
        ).toHaveCount(0, {timeout: 30_000});
    });
});
