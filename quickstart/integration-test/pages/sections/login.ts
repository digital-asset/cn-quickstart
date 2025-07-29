import {expect, type Page, type Locator} from '@playwright/test';
import {APP_PROVIDER_LOGIN_URL, DEFAULT_PASSWORD} from '../../tests/global.ts';

export default class Login {
  page: Page;

  constructor(page: Page) {
    this.page = page;
  }

  public async login(user: string, displayName: string): Promise<void> {
    await this.page.goto(APP_PROVIDER_LOGIN_URL);
    await this.page.getByRole('link', {name: 'AppUser'}).click();
    await this.page.getByRole('textbox', {name: 'Username or email'}).fill(user);
    await this.page.getByRole('textbox', {name: 'Password'}).fill(DEFAULT_PASSWORD);
    await this.page.getByRole('button', {name: 'Sign In'}).click();
    await expect(this.page.locator('#user-name')).toHaveText(displayName);
  }
}
