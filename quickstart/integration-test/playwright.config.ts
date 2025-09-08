import { defineConfig} from '@playwright/test';
import * as dotenv from 'dotenv';
import {existsSync} from 'fs';
import * as path from 'path';

const { PROVIDER_STORAGE } = require('./tests/global.ts');

const envPath = path.resolve(__dirname, '.generated.env');
if (!existsSync(envPath)) {
  throw new Error("Run Quickstart in TEST_MODE please.");
}
dotenv.config({path: envPath});


/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './tests',
  /* Timeouts */
  timeout: 60_000,
  expect: {
    timeout: 20_000,
  },

  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['list'],
    ['html', {open: 'never'}],
    ['junit', { outputFile: 'results.xml' }]
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    browserName: 'chromium',
    actionTimeout: 20_000,
    // default timeout for navigation/waitForNavigation
    navigationTimeout: 30_000,

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'retain-on-failure',
  },

  projects: [
    {
      name: 'login',
      testMatch: '**/login.spec.ts',
    },

    {
      name: 'workflow',
      testMatch: '**/workflow.spec.ts',
      dependencies: ['login'],
    },
    {
      name: 'tenant-registrations',
      testMatch: '**/tenant-registration.spec.ts',
      dependencies: ['login'],
      use: {
        storageState: PROVIDER_STORAGE,
      },
    },
  ],
});
