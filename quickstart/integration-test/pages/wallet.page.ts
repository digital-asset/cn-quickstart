import {expect, test, type Page, type Locator} from '@playwright/test';
import {WALLET_URL, DEFAULT_PASSWORD} from '../tests/global';
import {Keycloak} from '../utils/keycloak';
import {onboardWalletUser} from '../utils/wallet';

export default class Wallet {
  page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  public async acceptAllocationRequest(amount: number, description: string, renewalRequestId: string): Promise<void> {
    const allocationRequest = this.page.locator('.allocation-request', { hasText: renewalRequestId });
    const table = await allocationRequest.getByRole('table');
    await expect(table.getByText(description)).toBeVisible();
    await expect(table.getByText(`${amount} Amulet`)).toBeVisible();
    await table.getByRole('button', { name: 'Accept' }).click();
  }

  public async onboardWalletUser(keycloak: Keycloak, userId: string, partyId: string): Promise<void> {
    const validator = 'localhost:2' + process.env.VALIDATOR_ADMIN_API_PORT_SUFFIX!;
    const walletAdminToken = await keycloak.getUserToken(
      process.env.AUTH_APP_USER_WALLET_ADMIN_USER_NAME!, 
      process.env.AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD!, 
      process.env.AUTH_APP_USER_AUTO_CONFIG_CLIENT_ID!
    );
    await onboardWalletUser(this.page.request, walletAdminToken, userId, partyId, validator);
  }

  public async login(): Promise<void> {
    await this.page.goto(WALLET_URL);
    await this.page.getByRole('button', {name: 'Log In with OAuth2'}).click();
    await expect(this.page.getByText('Please re-authenticate to continue')).toBeVisible();
    await this.page.getByRole('textbox', {name: 'Password'}).fill(DEFAULT_PASSWORD);
    await this.page.getByRole('button', {name: 'Sign In'}).click();
    await expect(this.page.locator('#logged-in-user').getByRole('textbox')).toHaveValue(/.*app-user::.*/);
  }

  public async tap(amount: number): Promise<void> {
    await this.page.goto(WALLET_URL);
    await this.page.getByRole('textbox', {name: 'Amount'}).fill(amount.toString());
    await this.page.getByRole('button', {name: 'Tap'}).click();
    test.slow(); // CC processing can take a while
    await expect(
        this.page.locator('.tx-row-balance_change'),
      'Balance update should show in transaction history.'
    ).toBeVisible();
  }

  public async waitForURL(url: string | RegExp): Promise<void> {
    await this.page.waitForURL(url);
  }
}
