import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '.generated.env') });
if (process.env.DOCKER_RUN === 'true')
  dotenv.config({ path: path.resolve(__dirname, '.generated.docker.override.env'), override: true });

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
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: [
    ['list'],
    ['html', { open: 'never' }]
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    actionTimeout: 20_000,
    // default timeout for navigation/waitForNavigation
    navigationTimeout: 30_000,

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on-first-retry',
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'login',
      testMatch: '**/login.spec.ts',
      use: { ...devices['Desktop Firefox'] },
    },

    {
      name: 'workflow',
      testMatch: '**/workflow.spec.ts',
      use: { ...devices['Desktop Firefox'] },
      dependencies: ['login'],
    }
  ],
});
