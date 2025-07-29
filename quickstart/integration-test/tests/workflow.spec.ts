// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import {test} from '../fixtures/workflow.ts';
import {Status, Button as InstallButton} from "../pages/sections/appInstalls.tab.ts";
import {Button as LicenseButton, Link as LicenseLink} from "../pages/sections/licenses.tab.ts";
import {Button as LicenseModalButton} from "../pages/sections/licenses.modal.ts";

test.describe('AppInstall and Licensing workflow', () => {

  test('Users can see newly added AppInstallRequest', async ({requestTag, provider, user}) => {
    await provider.installs.goto();
    await provider.installs.assertMatchingRowCountIs(1, provider.installs.findRowBy(requestTag));
    await user.installs.goto();
    await user.installs.assertMatchingRowCountIs(1, user.installs.findRowBy(requestTag));
  });

  test('AppProvider can accept an AppInstallRequest', async ({requestTag, provider}) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.assertStatus(Status.Request);
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
    });
  });

  test('AppProvider can reject an AppInstallRequest', async ({requestTag, provider}) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.assertStatus(Status.Request);
      await provider.installs.clickButton(InstallButton.Reject);
      await provider.installs.assertNoMatchingRowExists();
    });
  });

  test('AppProvider can cancel an AppInstall', async ({requestTag, provider}) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.assertStatus(Status.Request);
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
      await provider.installs.clickButton(InstallButton.Cancel);
      await provider.installs.assertNoMatchingRowExists();
    });
  });

  test('AppUser can see accepted AppInstall', async ({requestTag, provider, user}) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
    });
    await user.installs.goto();
    await user.installs.assertMatchingRowCountIs(1, user.installs.findRowBy(requestTag));
  });

  test('AppUser can cancel an AppInstall', async ({requestTag, provider, user}) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
    });

    await user.installs.goto();
    await user.installs.withRowMatching(requestTag, async () => {
      await user.installs.clickButton(InstallButton.Cancel);
      await user.installs.assertNoMatchingRowExists();
    });
  });

  test('AppProvider can create licenses', async ({requestTag, provider}) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(1);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(2);
    });
  });

  test('AppUser can see created licenses', async ({requestTag, provider, user}) => {
    let licenseIds: string[] = [];
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[0] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(1);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[1] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(2);
    });

    await user.licenses.goto();
    for (const licenseId of licenseIds) {
      await user.licenses.assertMatchingRowCountIs(1, user.licenses.findRowBy(licenseId));
    }
  });

  test('AppUser sees no license action buttons on non-renewable license', async ({requestTag, provider, user}) => {
    let licenseIds: string[] = [];
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[0] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(1);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[1] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(2);
    });

    await user.licenses.goto();
    await user.licenses.withRowMatching(licenseIds[1], async () => {
      await user.licenses.assertMatchingRowCountIs(1);
      await user.licenses.assertButtonDoesNotExist(LicenseButton.PayRenewal)
    });
  });

  test('AppProvider can expire a license', async ({requestTag, provider}) => {
    let licenseIds: string[] = [];
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(Status.Install);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[0] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(1);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[1] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(2);
    });

    await provider.licenses.goto();
    await provider.licenses.withRowMatching(licenseIds[1], async () => {
      await provider.licenses.clickButton(LicenseButton.Actions);
      await provider.licenses.modal.fillDescription('e.g. "License expired"', 'Testing license expiration');
      await provider.licenses.modal.clickButton(LicenseModalButton.Expire);
      await provider.waitForSuccessMessage('License expired successfully');
      await provider.licenses.assertNoMatchingRowExists();
    });
  });

  test('Full License Lifecycle should pass', async ({requestTag, keycloak, provider, user, appUser}) => {
    let licenseId!: string;
    await test.step('AppProvider can accept AppInstallRequest and create License', async () => {
      await provider.installs.goto();
      await provider.installs.withRowMatching(requestTag, async () => {
        await provider.installs.clickButton(InstallButton.Accept);
        await provider.installs.assertStatus(Status.Install);
        await provider.installs.clickButton(InstallButton.CreateLicense);
        licenseId = await provider.installs.captureLicenseId();
      });
    });

    const renewalReason = 'test renewal reason';
    await test.step('AppProvider can create License Renewal', async () => {
      await provider.licenses.goto();
      await provider.licenses.withRowMatching(licenseId, async () => {
        await provider.licenses.clickButton(LicenseButton.Actions);
        await provider.licenses.modal.fillDescription('e.g. "Renew for next month"', renewalReason);
        await provider.licenses.modal.clickButton(LicenseModalButton.Renewal);
        await provider.waitForSuccessMessage('License Renewal initiated successfully');
      });
    });

    await test.step('Onboard AppUser and tap some funds to wallet', async () => {
      await user.wallet.onboardWalletUser(keycloak, appUser.userId, appUser.partyId);
      await user.wallet.login();
      await user.wallet.tap(1000);
    });

    await test.step('AppUser can pay License Renewal through wallet', async () => {
      await user.licenses.goto();
      await user.licenses.withRowMatching(licenseId, async () => {
        await user.licenses.clickLink(LicenseLink.PayRenewal);
        await user.licenses.waitForURL(/.*wallet.localhost.*/);
        await user.wallet.pay(100, renewalReason);
        await user.licenses.waitForURL(/.*app-provider.localhost.*/);
      });
    });

    await test.step('AppProvider can complete License Renewal', async () => {
      await provider.licenses.goto();
      await provider.licenses.withRowMatching(licenseId, async () => {
        await provider.licenses.clickButton(LicenseButton.CompleteRenewal);
        await provider.waitForSuccessMessage('License renewal completed successfully');
      });
    });
  });
});
