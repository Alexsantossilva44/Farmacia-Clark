const TOKEN_KEY = 'farmacia_token'
const EXPIRES_KEY = 'farmacia_token_expires'

export function saveToken(token: string, expiraEmSegundos: number): void {
  sessionStorage.setItem(TOKEN_KEY, token)
  sessionStorage.setItem(EXPIRES_KEY, String(Date.now() + expiraEmSegundos * 1000))
}

export function getToken(): string | null {
  const token = sessionStorage.getItem(TOKEN_KEY)
  const expires = sessionStorage.getItem(EXPIRES_KEY)
  if (!token || !expires) return null
  if (Date.now() > Number(expires)) {
    clearToken()
    return null
  }
  return token
}

export function clearToken(): void {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(EXPIRES_KEY)
}

export function isAuthenticated(): boolean {
  return getToken() !== null
}

export function getAuthHeader(): HeadersInit {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1]
    if (!base64) return null
    return JSON.parse(atob(base64)) as Record<string, unknown>
  } catch {
    return null
  }
}

export function getUserEmail(): string | null {
  const token = getToken()
  if (!token) return null
  const payload = decodeJwtPayload(token)
  return typeof payload?.sub === 'string' ? payload.sub : null
}

export function getFuncionarioId(): string | null {
  const token = getToken()
  if (!token) return null
  const payload = decodeJwtPayload(token)
  const id = payload?.funcionarioId
  return typeof id === 'string' ? id : null
}

export function getUserName(): string | null {
  const token = getToken()
  if (!token) return null
  const payload = decodeJwtPayload(token)
  return typeof payload?.nome === 'string' ? payload.nome : null
}

export function getUserRoles(): string[] {
  const token = getToken()
  if (!token) return []
  const payload = decodeJwtPayload(token)
  const authorities = payload?.authorities ?? payload?.roles
  if (Array.isArray(authorities)) {
    return authorities.map(String)
  }
  return []
}

export function hasAnyRole(...roles: string[]): boolean {
  const userRoles = getUserRoles()
  return roles.some((r) => userRoles.includes(r))
}

export function canValidarReceita(): boolean {
  return hasAnyRole('ROLE_FARMACEUTICO', 'ROLE_GERENTE', 'ROLE_ADMIN')
}

export function canGerenciarMedicamentos(): boolean {
  return hasAnyRole('ROLE_GERENTE', 'ROLE_ADMIN')
}

export function canGerenciarEstoque(): boolean {
  return hasAnyRole('ROLE_ESTOQUISTA', 'ROLE_GERENTE', 'ROLE_ADMIN')
}

export function canGerenciarCompras(): boolean {
  return hasAnyRole('ROLE_ESTOQUISTA', 'ROLE_GERENTE', 'ROLE_ADMIN')
}

export function isAdmin(): boolean {
  return hasAnyRole('ROLE_ADMIN')
}
