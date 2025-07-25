import { expect, type Page, type Locator } from '@playwright/test';
import AppInstalls from "./sections/appInstalls.tab";
import Licenses from "./sections/licenses.tab";
import Wallet from './wallet.page.ts';
import Login from './sections/login.ts';
import type {APIRequestContext} from 'playwright-core';

export default class QS {
    loginPage: Login
    installs: AppInstalls
    licenses: Licenses
    wallet: Wallet
    private page: Page;

    constructor(page: Page, request: APIRequestContext) {
        this.page = page;
        this.loginPage = new Login(page);
        this.installs = new AppInstalls(page);
        this.licenses = new Licenses(page);
        this.wallet = new Wallet(page, request);
    }

    public async waitForSuccessMessage(message: string): Promise<void> {
        const success = this.page.getByText(`Success: ${message}`);
        await expect(success).toBeVisible();
        await this.page.getByRole('button', { name: 'Close' }).click();
    }    
}