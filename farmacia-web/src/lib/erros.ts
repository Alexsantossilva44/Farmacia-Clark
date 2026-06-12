import { ApiError } from './api'
import { resolverMensagemErroApi, traduzirTextoConhecido } from './mensagens'

/**
 * Converte mensagens técnicas (inglês) em texto amigável pt-BR para a UI.
 */
export function traduzirErroApi(erro: unknown): string {
  if (erro instanceof ApiError) {
    const campos = erro.problem?.fields?.filter((f) => f.userMessage)
    if (campos?.length) {
      return campos.map((f) => f.userMessage).join(' ')
    }
    return resolverMensagemErroApi(
      erro.status,
      erro.problem?.detail ?? erro.problem?.title ?? erro.message,
      erro.problem?.userMessage,
    )
  }

  if (erro instanceof Error) {
    return traduzirTextoConhecido(erro.message)
  }

  return 'Ocorreu um erro inesperado.'
}

export { mensagemValidacaoCampo } from './mensagens'
