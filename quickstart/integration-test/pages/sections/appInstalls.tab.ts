import {expect, type Page, type Locator} from '@playwright/test';
import RowOps from '../../utils/rowOps';

const APP_PROVIDER_APP_INSTALLS_URL = 'http://app-provider.localhost:3000/app-installs';

export enum Status {
  AwaitingAcceptance = 'AWAITING_ACCEPTANCE',
  Accepted = 'ACCEPTED',
}

export enum Button {
  Accept = 'Accept',
  Cancel = 'Cancel',
  CreateLicense = 'Create License',
  Reject = 'Reject',
}

export default class AppInstalls extends RowOps {
  constructor(page: Page) {
    super(page);
  }

  button = (name: string, row: Locator = this.matchingRow): Locator => {
    return row.getByRole('button', {name: name});
  }

  status = (name: Status, row: Locator = this.matchingRow): Locator => {
    return row.getByRole('cell', {name: name});
  }

  licenseCount = (row: Locator = this.matchingRow): Locator => {
    return row.getByTestId('num-licenses');
  }

  public async goto(): Promise<void> {
    await this.page.goto(APP_PROVIDER_APP_INSTALLS_URL);
  }

  public async assertStatus(status: Status, row: Locator = this.matchingRow): Promise<void> {
    await expect(this.status(status, row)).toBeVisible();
  }

  public async clickButton(button: Button, row: Locator = this.matchingRow): Promise<void> {
    await this.button(button, row).click();
  }

  public async assertLicenseCountIs(licenseCount: number, row: Locator = this.matchingRow): Promise<void> {
    await expect(
      this.licenseCount(row),
      `There should be ${licenseCount} license(s) for the AppInstall`
    ).toHaveText(licenseCount.toString());
  }

  public async captureLicenseId(): Promise<string> {
    // wait for the success message to appear
    const success = this.page.getByText(/Success: Created License:/);
    await expect(success).toBeVisible();

    // read its textContent
    const fullText = await success.textContent();
    // e.g. "Success: Created License: 00a6f3cf…"

    // extract just the ID
    const match = fullText!.match(/Success: Created License: ([0-9a-f]+)/);
    expect(match, 'license ID should parse').not.toBeNull();
    const licenseId = match![1];
    console.log('Captured new LicenseId:', licenseId);

    await this.page.getByRole('button', {name: 'Close'}).click();

    return licenseId;
  }
}
