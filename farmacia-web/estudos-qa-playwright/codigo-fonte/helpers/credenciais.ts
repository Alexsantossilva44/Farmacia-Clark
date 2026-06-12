// Cópia para estudo — arquivo original: farmacia-web/e2e/helpers/credenciais.ts
/** Credenciais do seed dev (`DevAmbienteSeed`) — só válidas com API em perfil `dev`. */
export const ADMIN = {
  email: 'admin@farmacia.com',
  senha: 'admin123',
} as const

export const FARMACIA_NOME_COMPLETO = 'Farmácia Clark'

/** Chaves alinhadas a `farmacia-web/src/lib/auth.ts` (sessão no navegador). */
export const TOKEN_KEY = 'farmacia_token'
export const TOKEN_EXPIRES_KEY = 'farmacia_token_expires'
