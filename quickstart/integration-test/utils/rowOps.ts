import {expect, type Page, type Locator} from '@playwright/test';

export default class RowOps {
  page: Page;
  protected matchingRow: Locator;

  constructor(page: Page) {
    this.page = page;
  }

  findRowBy = (pattern: string | RegExp): Locator => {
    const nameMatcher = pattern instanceof RegExp ? pattern : new RegExp(pattern);
    const cell = this.page.getByRole('cell', {name: nameMatcher});
    return this.page.getByRole('row').filter({ has: cell });
  }

  public async withRowMatching(
    pattern: string | RegExp,
    withRow: (row: Locator) => Promise<void>
  ): Promise<void> {
    const row = this.findRowBy(pattern)
    this.matchingRow = row;
    await withRow(row)
  }

  public async assertNoMatchingRowExists(row: Locator = this.matchingRow): Promise<void> {
    await expect(
      row,
      'There shouldn\'t be any matching row.'
    ).toHaveCount(0);
  }

  public async assertMatchingRowCountIs(count: number, row: Locator = this.matchingRow): Promise<void> {
    await expect(
      row,
      `There should be exactly ${count} matching row(s).`
    ).toHaveCount(count);
  }
}
