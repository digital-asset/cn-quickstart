import { expect, test, type Page, type Locator } from '@playwright/test';
import type {APIRequestContext} from 'playwright-core';
import { WALLET_URL, DEFAULT_PASSWORD} from '../tests/global.ts';
import { getUserToken} from '../utils/keycloak.ts';
import { onboardWalletUser} from '../utils/wallet.ts';

export default class Wallet {
  page: Page;
  request: APIRequestContext;
  constructor(page: Page, request: APIRequestContext) {
    this.page = page;
    this.request = request;
  }

  public async pay(amount: number, description: string): Promise<void> {
    await expect(this.page.locator('.payment-description', {hasText: description})).toBeVisible();
    await expect(this.page.locator('#confirm-payment')).toContainText(`Send ${amount} CC to`);
    await this.page.getByRole('button', { name: 'Send Payment' }).click();
  }

  public async onboardWalletUser(userId: string, partyId: string): Promise<void> {
    const validator = 'localhost:2' + process.env.VALIDATOR_ADMIN_API_PORT_SUFFIX!;
    const walletAdminToken = await getUserToken(this.request, process.env.AUTH_APP_USER_WALLET_ADMIN_USER_NAME!, process.env.AUTH_APP_USER_WALLET_ADMIN_USER_PASSWORD!, process.env.AUTH_APP_USER_AUTO_CONFIG_CLIENT_ID!);
    await onboardWalletUser(this.request, walletAdminToken, userId, partyId, validator);
  }

  public async login(): Promise<void> {
    await this.page.goto(WALLET_URL);
    await this.page.getByRole('button', { name: 'Log In with OAuth2' }).click();
    await expect(this.page.getByText('Please re-authenticate to continue')).toBeVisible();
    await this.page.getByRole('textbox', { name: 'Password' }).fill(DEFAULT_PASSWORD);
    await this.page.getByRole('button', { name: 'Sign In' }).click();
    await expect(this.page.locator('#logged-in-user').getByRole('textbox')).toHaveValue(/.*app-user::.*/);
  }

  public async tap(amount: number): Promise<void> {
    await this.page.goto(WALLET_URL);
    await this.page.getByRole('textbox', { name: 'Amount' }).fill(amount.toString());
    await this.page.getByRole('button', { name: 'Tap' }).click();
    test.slow(); // CC processing can take a while
    await expect(
        this.page.locator('[data-testid="AccountBalanceWalletIcon"]').first(),
        'Balance update should show in transaction history.'
    ).toBeVisible();
  }  
}
