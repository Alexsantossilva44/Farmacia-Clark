import { expect, type Page } from '@playwright/test'
import { linkMenu } from './navegacao'

/** Viewport abaixo de `lg` (1024px) — drawer em vez de sidebar fixa. */
export const MOBILE_VIEWPORT = { width: 390, height: 844 } as const

export function isViewportMobile(page: Page) {
  const size = page.viewportSize()
  return size != null && size.width < 1024
}

/** Abre o menu lateral (drawer) no mobile. */
export async function abrirMenuMobile(page: Page) {
  await page.getByRole('button', { name: 'Abrir menu' }).click()
  await expect(page.getByRole('complementary', { name: 'Menu principal' })).toBeVisible()
}

/** Navega pelo drawer mobile (abre menu + clica no link). */
export async function navegarMenuMobile(page: Page, nomeLink: string) {
  await abrirMenuMobile(page)
  await linkMenu(page, nomeLink).click()
}
