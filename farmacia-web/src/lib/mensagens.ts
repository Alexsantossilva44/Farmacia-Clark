const HTTP_STATUS_PT: Record<number, string> = {
  400: 'Requisição inválida',
  401: 'Não autorizado',
  403: 'Acesso negado',
  404: 'Não encontrado',
  409: 'Conflito — operação não permitida',
  422: 'Dados inválidos',
  500: 'Erro interno do servidor',
  502: 'Servidor indisponível — a API não está respondendo',
  503: 'Serviço temporariamente indisponível',
  504: 'Tempo esgotado ao aguardar resposta da API',
}

const TEXTO_INGLES_PT: Record<string, string> = {
  Unauthorized: 'Não autorizado',
  'Bad Request': 'Requisição inválida',
  Forbidden: 'Acesso negado',
  'Not Found': 'Não encontrado',
  Conflict: 'Conflito',
  'Internal Server Error': 'Erro interno do servidor',
  'Bad Gateway': 'Servidor indisponível — a API não está respondendo',
  'Service Unavailable': 'Serviço temporariamente indisponível',
  'Gateway Timeout': 'Tempo esgotado ao aguardar resposta da API',
  'Failed to fetch': 'Falha ao conectar com o servidor',
  'NetworkError when attempting to fetch resource.': 'Falha de rede ao contactar o servidor',
  'Load failed': 'Falha ao carregar dados do servidor',
  'Network request failed': 'Falha na requisição de rede',
}

const DICA_BACKEND =
  ' Inicie o backend com: mvn spring-boot:run -pl farmacia-api (e docker compose up -d).'

export function traduzirTextoConhecido(texto: string): string {
  const normalizado = texto.trim()
  if (!normalizado) return 'Ocorreu um erro inesperado.'
  if (TEXTO_INGLES_PT[normalizado]) return TEXTO_INGLES_PT[normalizado]

  if (/bad gateway/i.test(normalizado)) {
    return TEXTO_INGLES_PT['Bad Gateway'] + DICA_BACKEND
  }

  if (/failed to fetch/i.test(normalizado) || /networkerror/i.test(normalizado)) {
    return (
      'Não foi possível conectar à API.' + DICA_BACKEND
    )
  }

  return normalizado
}

export function mensagemErroHttp(status: number): string {
  const base = HTTP_STATUS_PT[status]
  if (!base) return 'Ocorreu um erro inesperado.'

  if (status === 502 || status === 503 || status === 504 || status === 0) {
    return base + DICA_BACKEND
  }
  return base
}

export function traduzirStatusHttp(status: number, statusText?: string): string {
  if (HTTP_STATUS_PT[status]) return mensagemErroHttp(status)
  if (statusText) return traduzirTextoConhecido(statusText)
  return 'Ocorreu um erro inesperado.'
}

export function mensagemValidacaoCampo(nome: string): string {
  return `Preencha o campo ${nome}.`
}

/**
 * Monta mensagem amigável pt-BR a partir da resposta HTTP da API.
 * Prioriza userMessage do backend (já em português), depois código HTTP, depois texto bruto.
 */
export function resolverMensagemErroApi(
  status: number,
  bruto?: string,
  userMessage?: string,
): string {
  if (userMessage?.trim()) return userMessage.trim()
  if (HTTP_STATUS_PT[status]) return mensagemErroHttp(status)
  if (bruto?.trim()) return traduzirTextoConhecido(bruto)
  return 'Ocorreu um erro inesperado.'
}
