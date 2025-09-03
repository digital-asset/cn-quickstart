import {test as base, type Page, type Locator} from '@playwright/test';
import AppUserSetup from '../utils/appUserSetup';
import QS from '../pages/qs.page';
import {PROVIDER_STORAGE} from '../tests/global';
import {Keycloak} from '../utils/keycloak';

type Fixtures = {
  keycloak: Keycloak
  tagProvider: TagProvider;
  appUserSetup: AppUserSetup;
  provider: QS;
  user: QS;
  requestTag: string;
};

export * from '@playwright/test';
export const test = base.extend<Fixtures>({
  keycloak: async ({}, use) => {
    const kc = await new Keycloak();
    await kc.init();
    await use(kc);
  },
  tagProvider: async ({}, use) => {
    const tag = new TagProvider();
    console.log(`Using test tag: ${tag.base}`);
    await use(tag);
  },
  appUserSetup: async ({request, keycloak, tagProvider}, use) => {
    // Create an AppUser test instance with a unique test tag
    // - creates keycloak user, ledger party, and ledger user
    // - grants rights to the user to act as and read as the party
    const appUser = await base.step('Create a unique test AppUser', async () => {
      return await AppUserSetup.create(request, keycloak, tagProvider);
    });
    await use(appUser);
  },
  requestTag: async ({tagProvider, appUserSetup}, use) => {
    const tag = tagProvider.next();
    await base.step('Run create-app-install-request script', async () => {
      appUserSetup.createAppInstallRequest(tag);
    });

    await use(tag);
  },
  provider: async ({browser}, use) => {
    const context = await browser.newContext({storageState: PROVIDER_STORAGE});
    const providerPage = await context.newPage();
    const provider = new QS(providerPage);
    await use(provider);
    await context.close();
  },
  user: async ({browser, appUserSetup, tagProvider}, use) => {
    const context = await browser.newContext();
    const userPage = await context.newPage();
    const user = new QS(userPage);
    // Login as the test user
    await user.loginPage.login(appUserSetup.userName, `app user ${tagProvider.base}`);
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