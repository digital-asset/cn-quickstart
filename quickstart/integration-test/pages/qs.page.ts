import {expect, type Page, type Locator} from '@playwright/test';
import AppInstalls from "./sections/appInstalls.tab";
import Licenses from "./sections/licenses/licenses.tab.ts";
import Wallet from './wallet.page.ts';
import Tenants from "./sections/tenants.tab";
import Login from './sections/login.ts';

export default class QS {
  loginPage: Login
  installs: AppInstalls
  licenses: Licenses
  tenants: Tenants
  wallet: Wallet
  private page: Page;

  constructor(page: Page) {
    this.page = page;
    this.loginPage = new Login(page);
    this.installs = new AppInstalls(page);
    this.licenses = new Licenses(page);
    this.tenants = new Tenants(page);
    this.wallet = new Wallet(page);
  }

  public async waitForSuccessMessage(message: string): Promise<void> {
    const success = this.page.getByText(`Success: ${message}`);
    await expect(success).toBeVisible();
    await this.page.locator('#liveToast').getByRole('button', { name: 'Close' }).click();
  }

  public async waitForURL(url: string | RegExp): Promise<void> {
    await this.page.waitForURL(url);
  }
}