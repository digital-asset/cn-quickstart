import { type Page, type Locator } from '@playwright/test';

export enum Button {
  Expire = 'Expire',
  Renewal = 'Issue Renewal Payment Request'
}

export default class LicensesModal {
  page: Page;
  constructor(page: Page) {
    this.page = page;
  }

  button = (name: Button): Locator => {
    return this.page.getByRole('button', { name: name });
  }

  public async clickButton(button: Button): Promise<void> {
     await this.button(button).click();
  }

  public async fillDescription(inputName: string, description: string): Promise<void> {
    const input = this.page.getByRole('textbox', { name: inputName });
    await input.click();
    await input.fill(description);
  }
}
