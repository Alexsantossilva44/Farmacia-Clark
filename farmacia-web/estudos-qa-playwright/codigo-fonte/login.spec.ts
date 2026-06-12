// Cópia para estudo — arquivo original: farmacia-web/e2e/login.spec.ts
import { test, expect } from '@playwright/test'
import { ADMIN, FARMACIA_NOME_COMPLETO } from './helpers/credenciais'

test.describe('Login — Farmácia Clark', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
  })

  test('exibe marca e formulário de acesso', async ({ page }) => {
    await expect(page).toHaveTitle(/Farmácia Clark/i)
    await expect(page.getByText(FARMACIA_NOME_COMPLETO).first()).toBeVisible()
    await expect(page.getByRole('heading', { name: /Entrar no sistema/i })).toBeVisible()
    await expect(page.getByTestId('login-email')).toBeVisible()
    await expect(page.getByTestId('login-senha')).toBeVisible()
    await expect(page.getByTestId('login-submit')).toBeEnabled()
  })

  test('rejeita credenciais inválidas', async ({ page }) => {
    await page.getByTestId('login-email').fill('invalido@farmacia.com')
    await page.getByTestId('login-senha').fill('senha-errada')
    await page.getByTestId('login-submit').click()

    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByText(/credenciais|inválid|erro/i).first()).toBeVisible({ timeout: 15_000 })
  })

  test('admin dev autentica e abre o painel', async ({ page }) => {
    await page.getByTestId('login-email').fill(ADMIN.email)
    await page.getByTestId('login-senha').fill(ADMIN.senha)
    await page.getByTestId('login-submit').click()

    await expect(page).toHaveURL('/')
    await expect(page.getByRole('heading', { name: /Painel operacional/i })).toBeVisible()
  })

  test('com Vite em modo dev exibe atalhos de contas de desenvolvimento', async ({ page }) => {
    await expect(page.getByText(/Contas de desenvolvimento/i)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Administrador' })).toBeVisible()
  })
})
