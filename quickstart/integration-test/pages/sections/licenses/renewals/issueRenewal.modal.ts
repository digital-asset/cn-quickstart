import {type Page, type Locator} from '@playwright/test';

export enum Button {
  IssueLicenseRenewalRequest = 'Issue License Renewal Request'
}

export default class IssueRenewalModal {
  page: Page;

  constructor(page: Page) {
    this.page = page;
  }


  button = (name: Button): Locator => {
    return this.page.getByRole('button', {name: name});
  }

  public async clickButton(button: Button): Promise<void> {
    await this.button(button).click();
  }

  public async fillDescription(description: string): Promise<void> {
    const input = this.page.getByRole('textbox');
    await input.click();
    await input.fill(description);
  }
}
