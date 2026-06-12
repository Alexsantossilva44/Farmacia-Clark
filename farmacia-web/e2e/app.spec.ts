import { test, expect } from '@playwright/test'
import { garantirSessaoAdmin } from './helpers/autenticar'
import { FARMACIA_NOME_COMPLETO } from './helpers/credenciais'
import { linkMenu } from './helpers/navegacao'

test.describe('App autenticado', () => {
  test.beforeEach(async ({ page, request }) => {
    await garantirSessaoAdmin(page, request)
  })
  test('sidebar exibe marca Farmácia Clark', async ({ page }) => {
    const menu = page.getByRole('complementary')
    await expect(menu.getByText(FARMACIA_NOME_COMPLETO.split(' ')[0])).toBeVisible()
    await expect(menu.getByText('Clark')).toBeVisible()
    await expect(linkMenu(page, 'Estoque')).toBeVisible()
  })

  test('navega para estoque e exibe listagem', async ({ page }) => {
    await page.goto('/estoque')
    await expect(page.getByRole('heading', { name: /Estoque & FEFO/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /Nova entrada/i })).toBeVisible()
    await expect(page.getByPlaceholder(/Buscar medicamento/i)).toBeVisible()
  })

  test('abre formulário de nova entrada de mercadoria', async ({ page }) => {
    await page.goto('/estoque')
    await page.getByRole('button', { name: /Nova entrada/i }).click()
    await expect(page.getByRole('heading', { name: /Entrada de mercadoria/i })).toBeVisible()
    await expect(page.getByText(/Somente hoje ou datas futuras/i)).toBeVisible()
  })

  test('navega para PDV', async ({ page }) => {
    await page.goto('/vendas')
    await expect(page.getByRole('heading', { name: /Nova venda/i })).toBeVisible()
  })
})
