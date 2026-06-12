import { test, expect } from '@playwright/test'
import { garantirSessaoAdmin } from './helpers/autenticar'
import { FARMACIA_NOME_COMPLETO } from './helpers/credenciais'
import { abrirMenuMobile, MOBILE_VIEWPORT, navegarMenuMobile } from './helpers/mobile'
import { linkMenu } from './helpers/navegacao'

test.describe('App autenticado — mobile', () => {
  test.beforeEach(async ({ page, request }) => {
    await page.setViewportSize(MOBILE_VIEWPORT)
    await garantirSessaoAdmin(page, request)
  })

  test('exibe barra superior e menu fechado por padrão', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Abrir menu' })).toBeVisible()
    await expect(page.getByRole('heading', { name: /Painel operacional/i })).toBeVisible()
    await expect(linkMenu(page, 'Estoque')).not.toBeVisible()
  })

  test('drawer abre e exibe marca e links de navegação', async ({ page }) => {
    await abrirMenuMobile(page)

    const menu = page.getByRole('complementary', { name: 'Menu principal' })
    await expect(menu.getByText(FARMACIA_NOME_COMPLETO.split(' ')[0])).toBeVisible()
    await expect(menu.getByText('Clark')).toBeVisible()
    await expect(linkMenu(page, 'Estoque')).toBeVisible()
    await expect(menu.getByRole('link', { name: 'PDV / Vendas' })).toBeVisible()
  })

  test('navega para estoque pelo drawer e fecha o menu', async ({ page }) => {
    await navegarMenuMobile(page, 'Estoque')

    await expect(page).toHaveURL(/\/estoque/)
    await expect(page.getByRole('heading', { name: /Estoque & FEFO/i })).toBeVisible()
    await expect(page.getByRole('button', { name: /Nova entrada/i })).toBeVisible()
    await expect(linkMenu(page, 'Estoque')).not.toBeVisible()
    await expect(page.getByRole('button', { name: 'Abrir menu' })).toBeVisible()
  })

  test('navega para PDV pelo drawer', async ({ page }) => {
    await navegarMenuMobile(page, 'PDV / Vendas')

    await expect(page).toHaveURL(/\/vendas/)
    await expect(page.getByRole('heading', { name: /Nova venda/i })).toBeVisible()
  })

  test('fecha drawer pelo botão X no menu', async ({ page }) => {
    await abrirMenuMobile(page)
    await expect(linkMenu(page, 'Estoque')).toBeVisible()

    await page
      .getByRole('complementary', { name: 'Menu principal' })
      .getByRole('button', { name: 'Fechar menu' })
      .click()

    await expect(linkMenu(page, 'Estoque')).not.toBeVisible()
    await expect(page.getByRole('button', { name: 'Abrir menu' })).toBeVisible()
  })
})
