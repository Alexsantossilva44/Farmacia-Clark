import { test as setup } from '@playwright/test'
import { garantirSessaoAdmin } from './helpers/autenticar'

/** Valida que API + front permitem autenticação antes dos testes autenticados. */
setup('pré-condição: API e sessão admin', async ({ page, request }) => {
  await garantirSessaoAdmin(page, request)
})
