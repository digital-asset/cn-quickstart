import {expect, type Page, type Locator} from '@playwright/test';
import RowOps from '../../utils/rowOps.ts';
import LicensesModal from './licenses.modal.ts';

const APP_PROVIDER_LICENSES_URL = 'http://app-provider.localhost:3000/licenses';

export enum Button {
  Actions = 'Actions',
  CompleteRenewal = 'Complete Renewal',
  PayRenewal = 'Pay Renewal'
}

export enum Link {
  PayRenewal = 'Pay Renewal'
}

export default class Licenses extends RowOps {
  modal: LicensesModal;

  constructor(page: Page) {
    super(page);
    this.modal = new LicensesModal(page);
  }

  button = (name: string, row: Locator = this.matchingRow): Locator => {
    return row.getByRole('button', {name: name});
  }

  public async assertButtonDoesNotExist(button: Button, row: Locator = this.matchingRow): Promise<void> {
    await expect(this.button(button, row)).not.toBeVisible();
  }

  public async assertButtonExists(button: Button, row: Locator = this.matchingRow): Promise<void> {
    await expect(this.button(button, row)).toBeVisible();
  }

  public async clickButton(button: Button, row: Locator = this.matchingRow): Promise<void> {
    await this.button(button, row).click();
  }

  public async clickLink(link: Link, row: Locator = this.matchingRow): Promise<void> {
    const linkLocator = row.getByRole('link').filter({hasText: link});
    await expect(linkLocator).toBeVisible();
    await linkLocator.click();
  }

  public async goto(): Promise<void> {
    await this.page.goto(APP_PROVIDER_LICENSES_URL);
  }

  public async waitForURL(url: string | RegExp): Promise<void> {
    await this.page.waitForURL(url);
  }
}
