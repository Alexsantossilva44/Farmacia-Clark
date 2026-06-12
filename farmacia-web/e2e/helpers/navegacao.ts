import type { Page } from '@playwright/test'

/** Link do menu lateral (evita ambiguidade com cards do painel). */
export function linkMenu(page: Page, nome: string) {
  return page.getByRole('complementary').getByRole('link', { name: nome, exact: true })
}
