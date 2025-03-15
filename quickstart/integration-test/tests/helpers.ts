import {Page, expect} from '@playwright/test';

/**
 * Returns a visible locator by the given selector. Fails if the element is not visible.
 */
export async function getVisibleLocator(
    page: Page,
    selector: string,
    message?: string
) {
    const locator = page.locator(selector);
    await expect(locator, message).toBeVisible();
    return locator;
}
