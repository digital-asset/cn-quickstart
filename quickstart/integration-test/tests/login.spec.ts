import {test, expect, BrowserContext, Page} from '@playwright/test';
// @ts-ignore
import {PROVIDER_STORAGE, USER_STORAGE} from './global.ts';
import {promises as fs} from 'fs';

// Reused runtime values
const APP_PROVIDER_LOGIN_URL = 'http://app-provider.localhost:3000/login';
const WALLET_URL = 'http://wallet.localhost:2000/';
const DEFAULT_PASSWORD = 'abc123';

test.describe('Authentication setup', () => {
  test.describe.configure({mode: 'serial'});

  let providerContext: BrowserContext;
  let providerPage: Page;
  let userContext: BrowserContext;
  let userPage: Page;

  test.beforeAll(async ({browser}) => {
    // Clean up any existing storage files
    await fs.rm(PROVIDER_STORAGE, {force: true});
    await fs.rm(USER_STORAGE, {force: true});

    // Create contexts from the newly written storage
    providerContext = await browser.newContext();
    providerPage = await providerContext.newPage();
    userContext = await browser.newContext();
    userPage = await userContext.newPage();
  });

  test.afterAll(async () => {
    await providerContext.storageState({path: PROVIDER_STORAGE});
    await userContext.storageState({path: USER_STORAGE});
    await providerContext.close();
    await userContext.close();
  });

  test('AppProvider can log in', async () => {
    await providerPage.goto(APP_PROVIDER_LOGIN_URL);
    // Has AppProvider login link
    await providerPage.getByRole('link', {name: 'AppProvider'}).click();
    await providerPage.getByRole('textbox', {name: 'Username or email'}).fill('app-provider');
    await providerPage.getByRole('textbox', {name: 'Password'}).fill(DEFAULT_PASSWORD);
    await providerPage.getByRole('button', {name: 'Sign In'}).click();
    await expect(providerPage.locator('#user-name')).toHaveText('app provider');
  });

  test('AppUser can log in', async () => {
    await userPage.goto(APP_PROVIDER_LOGIN_URL);
    // Has AppUser login link
    await userPage.getByRole('link', {name: 'AppUser'}).click();
    await userPage.getByRole('textbox', {name: 'Username or email'}).fill('app-user');
    await userPage.getByRole('textbox', {name: 'Password'}).fill(DEFAULT_PASSWORD);
    await userPage.getByRole('button', {name: 'Sign In'}).click();
    await expect(userPage.locator('#user-name')).toHaveText('app user');
  });

  test('AppUser can sign in to wallet UI', async () => {
    await userPage.goto(WALLET_URL);
    await userPage.getByRole('button', {name: 'Log In with OAuth2'}).click();
    await expect(userPage.getByText('Please re-authenticate to continue')).toBeVisible();
    await userPage.getByRole('textbox', {name: 'Password'}).fill(DEFAULT_PASSWORD);
    await userPage.getByRole('button', {name: 'Sign In'}).click();
    await expect(userPage.locator('#logged-in-user').getByRole('textbox')).toHaveValue(/^app_user_/);
  });
});
