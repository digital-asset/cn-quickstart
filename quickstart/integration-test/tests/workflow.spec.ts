// Copyright (c) 2026, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

import { Locator, test } from '../fixtures/workflow';
import { Status as InstallStatus, Button as InstallButton } from "../pages/sections/appInstalls.tab";
import { Button as LicenseButton } from "../pages/sections/licenses/licenses.tab";
import { Button as ArchiveModalButton } from "../pages/sections/licenses/archive.modal";
import { Button as RenewalsModalButton, Link as RenewalsModalLink, Status as RenewalsModalStatus } from "../pages/sections/licenses/renewals/renewals.modal";
import { Button as IssueRenewalModalButton } from "../pages/sections/licenses/renewals/issueRenewal.modal";


test.describe('AppInstall and Licensing workflow', () => {

  test('Users can see newly added AppInstallRequest', async ({ requestTag, provider, user }) => {
    await provider.installs.goto();
    await provider.installs.assertMatchingRowCountIs(1, provider.installs.findRowBy(requestTag));
    await user.installs.goto();
    await user.installs.assertMatchingRowCountIs(1, user.installs.findRowBy(requestTag));
  });

  test('AppProvider can accept an AppInstallRequest', async ({ requestTag, provider }) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.assertStatus(InstallStatus.AwaitingAcceptance);
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
    });
  });

  test('AppProvider can reject an AppInstallRequest', async ({ requestTag, provider }) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.assertStatus(InstallStatus.AwaitingAcceptance);
      await provider.installs.clickButton(InstallButton.Reject);
      await provider.installs.assertNoMatchingRowExists();
    });
  });

  test('AppProvider can cancel an AppInstall', async ({ requestTag, provider }) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.assertStatus(InstallStatus.AwaitingAcceptance);
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
      await provider.installs.clickButton(InstallButton.Cancel);
      await provider.installs.assertNoMatchingRowExists();
    });
  });

  test('AppUser can see accepted AppInstall', async ({ requestTag, provider, user }) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
    });
    await user.installs.goto();
    await user.installs.assertMatchingRowCountIs(1, user.installs.findRowBy(requestTag));
  });

  test('AppUser can cancel an AppInstall', async ({ requestTag, provider, user }) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
    });

    await user.installs.goto();
    await user.installs.withRowMatching(requestTag, async () => {
      await user.installs.clickButton(InstallButton.Cancel);
      await user.installs.assertNoMatchingRowExists();
    });
  });

  test('AppProvider can create licenses', async ({ requestTag, provider }) => {
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(1);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(2);
    });
  });

  test('AppUser can see created licenses', async ({ requestTag, provider, user }) => {
    let licenseIds: string[] = [];
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
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

  test('AppProvider can archive a license', async ({ requestTag, provider }) => {
    let licenseIds: string[] = [];
    await provider.installs.goto();
    await provider.installs.withRowMatching(requestTag, async () => {
      await provider.installs.clickButton(InstallButton.Accept);
      await provider.installs.assertStatus(InstallStatus.Accepted);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[0] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(1);
      await provider.installs.clickButton(InstallButton.CreateLicense);
      licenseIds[1] = await provider.installs.captureLicenseId();
      await provider.installs.assertLicenseCountIs(2);
    });

    await provider.licenses.goto();
    await provider.licenses.withRowMatching(licenseIds[1], async () => {
      await provider.licenses.clickButton(LicenseButton.Archive);
      await provider.licenses.archiveModal.fillDescription('Testing license archival');
      await provider.licenses.archiveModal.clickButton(ArchiveModalButton.Archive);
      await provider.waitForSuccessMessage('License archived successfully');
      await provider.licenses.assertNoMatchingRowExists();
    });
  });

  test('Full License Lifecycle should pass', async ({ requestTag, keycloak, provider, user, appUserSetup }) => {
    let licenseId!: string;
    await test.step('AppProvider can accept AppInstallRequest and create License', async () => {
      await provider.installs.goto();
      await provider.installs.withRowMatching(requestTag, async () => {
        await provider.installs.clickButton(InstallButton.Accept);
        await provider.installs.assertStatus(InstallStatus.Accepted);
        await provider.installs.clickButton(InstallButton.CreateLicense);
        licenseId = await provider.installs.captureLicenseId();
      });
    });

    const renewalReason = 'test renewal reason';
    await test.step('AppProvider can create License Renewal', async () => {
      const licenses = provider.licenses;
      await licenses.goto();
      await licenses.withRowMatching(licenseId, async () => {
        await licenses.clickButton(LicenseButton.Renewals);
        const renewals = licenses.renewalsModal;
        await renewals.clickButton(RenewalsModalButton.New, renewals.modal);
        await renewals.issueRenewalModal.fillDescription(renewalReason);
        await renewals.issueRenewalModal.clickButton(IssueRenewalModalButton.IssueLicenseRenewalRequest);
        await provider.waitForSuccessMessage('License Renewal initiated successfully');
      });
    });

    await test.step('Onboard AppUser and tap some funds to wallet', async () => {
      await user.wallet.onboardWalletUser(keycloak, appUserSetup.userId, appUserSetup.partyId);
      await user.wallet.login();
      await user.wallet.tap(1000);
    });

    let renewalRequestId!: string;
    await test.step('AppUser can pay License Renewal through wallet', async () => {
      const licenses = user.licenses;
      await licenses.goto();
      await licenses.withRowMatching(licenseId, async () => {
        await licenses.clickButton(LicenseButton.Renewals);
        const renewals = licenses.renewalsModal;
        await renewals.withRowMatching(RenewalsModalStatus.AwaitingAcceptance, async () => {
          renewalRequestId = await renewals.getRequestId();
          const wallet = await renewals.clickLink(RenewalsModalLink.WalletLink);
          await wallet.waitForURL(/.*wallet.localhost.*allocations/);
          await wallet.acceptAllocationRequest(100, renewalReason, renewalRequestId);
        });
      });
    });

    await test.step('AppProvider can complete License Renewal', async () => {
      const licenses = provider.licenses;
      await licenses.withRowMatching(licenseId, async () => {
        const renewals = licenses.renewalsModal;
        await renewals.withRowMatching(renewalRequestId, async () => {
          await renewals.clickButton(RenewalsModalButton.CompleteRenewal);
          await provider.waitForSuccessMessage('License renewal completed successfully');
        });
      });
    });
  });
});
