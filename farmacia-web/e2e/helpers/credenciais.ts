/** Credenciais do seed dev (`DevAmbienteSeed`) — só válidas com API em perfil `dev`. */
export const ADMIN = {
  email: 'admin@farmacia.com',
  senha: 'admin123',
} as const

/** Gerente — cadastros (fabricantes/categorias) e gestão operacional. */
export const GERENTE = {
  email: 'gerente@farmacia.com',
  senha: 'ger123',
} as const

export const FARMACEUTICO = {
  email: 'farmaceutico@farmacia.com',
  senha: 'farm123',
} as const

/** Estoquista — estoque e compras (canGerenciarEstoque / canGerenciarCompras). */
export const ESTOQUISTA = {
  email: 'estoquista@farmacia.com',
  senha: 'est123',
} as const

export const BALCONISTA = {
  email: 'balconista@farmacia.com',
  senha: 'bal123',
} as const

export const FARMACIA_NOME_COMPLETO = 'Farmácia Clark'

/** Chaves alinhadas a `farmacia-web/src/lib/auth.ts` (sessão no navegador). */
export const TOKEN_KEY = 'farmacia_token'
export const TOKEN_EXPIRES_KEY = 'farmacia_token_expires'
