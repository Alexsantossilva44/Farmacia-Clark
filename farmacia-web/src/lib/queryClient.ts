import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

/** Limpa cache ao trocar de usuário (evita contexto do login anterior). */
export function limparCacheUsuario() {
  queryClient.removeQueries({ queryKey: ['auth-contexto'] })
}
