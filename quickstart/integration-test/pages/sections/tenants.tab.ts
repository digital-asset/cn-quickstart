// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import { expect, type Page, type Locator } from '@playwright/test';
// @ts-ignore
import { TENANTS_URL } from '../../tests/global.ts';


import RowOps from "../../utils/rowOps";

const DEFAULT_OIDC_ISSUER = 'http://keycloak.localhost:8082/realms/AppProvider';

type AuthMode = 'oauth2' | 'shared-secret';

export enum Button {
    Submit = 'Submit',
    Delete = 'Delete'
}


export default class Tenants extends RowOps {
    constructor(page: Page) {
        super(page);
    }

    // Navigation
    public async goto(): Promise<void> {
        await this.page.goto(TENANTS_URL);
        await expect(
            this.page.getByRole('heading', { name: /Existing Tenant Registrations/i }),
        ).toBeVisible();
    }

    // Environment helpers
    async getAuthMode(): Promise<AuthMode> {
        return (process.env.AUTH_MODE as AuthMode) ?? 'oauth2';
    }

    async stubConfirmAlwaysTrue(): Promise<void> {
        await this.page.addInitScript(() => {
            // @ts-ignore
            window.confirm = () => true;
        });
    }

    // Form filling
    async fillCommon({
        tenantId,
        partyId,
        walletUrl,
    }: {
        tenantId: string;
        partyId: string;
        walletUrl: string;
    }): Promise<void> {
        await this.page.getByLabel(/Tenant ID:/i).fill(tenantId);
        await this.page.getByLabel(/PartyId:/i).fill(partyId);
        await this.page.getByLabel(/Wallet URL:/i).fill(walletUrl);
    }

    async fillOAuth2({
        clientId,
        issuerUrl = DEFAULT_OIDC_ISSUER,
    }: {
        clientId: string;
        issuerUrl?: string;
    }): Promise<void> {
        await this.page.getByLabel(/Client ID:/i).fill(clientId);
        await this.page.getByLabel(/Issuer URL:/i).fill(issuerUrl ?? '');
    }

    async fillSharedSecret({ users }: { users: string }): Promise<void> {
        await this.page.getByLabel(/Users \(comma-separated\):/i).fill(users);
    }

    async fillSecuritySection({
        clientId = `client-${Date.now()}`,
        issuerUrl = DEFAULT_OIDC_ISSUER,
        users = 'alice, bob',
    }): Promise<void> {
        const authMode = await this.getAuthMode();
        if (authMode === 'oauth2') {
            await this.fillOAuth2({ clientId, issuerUrl });
        } else {
            await this.fillSharedSecret({ users });
        }
    }

    // Submission + state
    private button = (name: string, row: Locator = this.matchingRow): Locator => {
        return row.getByRole('button', { name: name });
    }

    public async clickButton(button: Button, row: Locator = this.matchingRow): Promise<void> {
        await this.button(button, row).click();
    }

    private submitButton(): Locator {
        return this.page.getByRole('button', { name: Button.Submit });
    }

    async clickSubmit(): Promise<void> {
        const submit = this.submitButton();
        await expect(submit).toBeEnabled();
        await submit.click();
    }

    async assertSubmitEnabled(): Promise<void> {
        await expect(this.submitButton()).toBeEnabled();
    }

    async assertErrorVisible(rx: string | RegExp): Promise<void> {
        await expect(this.page.getByText(rx)).toBeVisible();
    }
}
