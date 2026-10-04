import { defineConfig } from '@playwright/test'

// Testler, ayakta olan gercek uygulamaya (docker compose up) karsi calisir:
// nginx -> Spring Boot -> PostgreSQL. Baska bir adres icin E2E_BASE_URL verilebilir.
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 7_000 },
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:3000',
    // Yerelde kurulu Chrome kullanilir; CI'da Playwright'in kendi Chromium'u indirilir
    channel: process.env.CI ? undefined : 'chrome',
    trace: 'retain-on-failure',
  },
})
