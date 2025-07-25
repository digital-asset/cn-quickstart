import { test as base, type Page, type Locator } from '@playwright/test';
import AppUser from '../utils/appUser.ts';
import QS from '../pages/qs.page.ts';
import { PROVIDER_STORAGE } from '../tests/global.ts';

type Fixtures = {
  tagProvider: TagProvider;
  appUser: AppUser;
  provider: QS;
  user: QS;
  requestTag: string;
};

export * from '@playwright/test';
export const test = base.extend<Fixtures>({
  tagProvider: async ({ }, use) => {
    const tag = new TagProvider();
    console.log(`Using test tag: ${tag.base}`);
    await use(tag);
  },
  appUser: async ({ request, tagProvider }, use) => {
    // Create an AppUser test instance with a unique test tag
    const appUser = await AppUser.create(request, tagProvider);
    await use(appUser);
  },
  requestTag: async ({ tagProvider, appUser }, use) => {
    const tag = tagProvider.next();
    await base.step('Run create-app-install-request script', async () => {
       appUser.createAppInstallRequest(tag);
    });

    await use(tag);
  },
  provider: async ({ browser, request }, use) => {
    const context = await browser.newContext({ storageState: PROVIDER_STORAGE });
    const providerPage = await context.newPage();
    const provider = new QS(providerPage, request);
    await use(provider);
    await context.close();
  },
  user: async ({ browser, request, appUser, tagProvider }, use) => {
    const context = await browser.newContext(); 
    const userPage = await context.newPage();
    const user = new QS(userPage, request);
    // Login as the test user
    await user.loginPage.login(appUser.userName, `app user ${tagProvider.base}`);
    await use(user);
    await context.close();
  },
});

export default class TagProvider {
  counter: number = 0;
  base: string = 'test-' + Date.now() + "-" + process.env.TEST_WORKER_INDEX;
  next(): string {
    this.counter++;
    return this.base + '-' + this.counter;
  }
  last(): string {
    return this.base + '-' + this.counter;
  }
}