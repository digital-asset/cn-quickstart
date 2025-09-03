import {expect, type Page, type Locator} from '@playwright/test';
import IssueRenewalModal from './issueRenewal.modal';
import RowOps from '../../../../utils/rowOps';
import Wallet from '../../../wallet.page';

export enum Button {
  New = 'New',
  Withdraw = 'Withdraw',
  CompleteRenewal = 'Complete Renewal'
}

export enum Link {
  WalletLink = 'wallet'
}

export enum Status {
  AwaitingAcceptance = 'AWAITING_ACCEPTANCE',
  Expired = 'EXPIRED',
}

export default class RenewalsModal extends RowOps {
  issueRenewalModal: IssueRenewalModal;
  modal: Locator;

  constructor(page: Page) {
    super(page);
    this.modal = this.page.getByRole('dialog');
    this.issueRenewalModal = new IssueRenewalModal(this.page);
  }

  button = (name: Button, loc: Locator = this.matchingRow): Locator => {
    return loc.getByRole('button', {name: name});
  }

  link = (link: Link, loc: Locator = this.matchingRow) : Locator => {
    return loc.getByRole('link').filter({hasText: link});
  }

  public async clickButton(button: Button, loc: Locator = this.matchingRow): Promise<void> {
    await this.button(button, loc).click();
  }

  public async clickLink(link: Link, loc: Locator = this.matchingRow): Promise<Wallet> {
    const linkLocator = this.link(link, loc);
    await expect(linkLocator).toBeVisible();

    // wait for the new tab (popup) to open as a result of the click
    const [newPage] = await Promise.all([
      this.page.waitForEvent('popup'),
      linkLocator.click(),
    ]);

    await newPage.waitForLoadState();
    return new Wallet(newPage);
  }

  public async getRequestId(row: Locator = this.matchingRow): Promise<string> {
    return row.getByTestId('renewal-request-id').innerText();
  }

  public async assertLinkDoesNotExist(link: Link, row: Locator = this.matchingRow): Promise<void> {
    await expect(this.link(link, row)).not.toBeVisible();
  }

}
