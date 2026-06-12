// Cópia para estudo — arquivo original: farmacia-web/playwright.config.ts
import { defineConfig, devices } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..')

const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173'
const apiBase = process.env.PLAYWRIGHT_API_URL ?? 'http://127.0.0.1:8080'
const apiHealthUrl = process.env.PLAYWRIGHT_API_HEALTH_URL ?? `${apiBase}/actuator/health`
const managedServers = process.env.CI === 'true' || process.env.PLAYWRIGHT_MANAGED_SERVERS === '1'

const apiJar = path.join(repoRoot, 'farmacia-api', 'target', 'farmacia-api-1.0.0-SNAPSHOT.jar')

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
  ],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
    },
    {
      name: 'login',
      testMatch: /login\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'app',
      testMatch: /app\.spec\.ts/,
      dependencies: ['setup'],
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: managedServers
    ? [
        {
          command: `java -jar "${apiJar}" --spring.profiles.active=dev`,
          url: apiHealthUrl,
          cwd: repoRoot,
          timeout: 180_000,
          reuseExistingServer: !process.env.CI,
        },
        {
          command: 'npm run dev',
          url: baseURL,
          cwd: __dirname,
          timeout: 60_000,
          reuseExistingServer: !process.env.CI,
        },
      ]
    : undefined,
})
