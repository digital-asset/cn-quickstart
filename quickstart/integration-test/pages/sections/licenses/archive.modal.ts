import {type Page, type Locator} from '@playwright/test';

export enum Button {
  Archive = 'Archive',
}

export default class ArchiveModal {
  page: Page;
  modal: Locator;

  constructor(page: Page) {
    this.page = page;
    this.modal = this.page.getByRole('dialog');
  }


  button = (name: Button): Locator => {
    return this.modal.getByRole('button', {name: name});
  }

  public async clickButton(button: Button): Promise<void> {
    await this.button(button).click();
  }

  public async fillDescription(description: string): Promise<void> {
    const input = this.modal.getByRole('textbox', {name: 'Description:'});
    await input.click();
    await input.fill(description);
  }
}
