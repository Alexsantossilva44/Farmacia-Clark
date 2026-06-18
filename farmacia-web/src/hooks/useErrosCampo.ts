import { useEffect, useRef, useState } from 'react'
import { MSG_OBRIGATORIO } from '@/lib/validacao-formulario'

export { MSG_OBRIGATORIO }

export const DELAY_AVISO_MS = 2000
export const DELAY_ERRO_MS = 3000

export function obrigatorio(valor: string): string | undefined {
  return valor.trim() ? undefined : MSG_OBRIGATORIO
}

export function calcularProgressoCampos(campos: boolean[]): number {
  if (!campos.length) return 0
  return Math.round((campos.filter(Boolean).length / campos.length) * 100)
}

export function useErrosCampo() {
  const [fieldErrors, setFieldErrors] = useState<Record<string, string | undefined>>({})
  const timersRef = useRef<Record<string, ReturnType<typeof setTimeout>>>({})

  useEffect(() => {
    const timers = timersRef.current
    return () => { Object.values(timers).forEach(clearTimeout) }
  }, [])

  function setErroTemporario(campo: string, mensagem: string | undefined, delay = DELAY_AVISO_MS) {
    setFieldErrors((prev) => ({ ...prev, [campo]: mensagem }))
    if (mensagem) {
      if (timersRef.current[campo]) clearTimeout(timersRef.current[campo])
      timersRef.current[campo] = setTimeout(
        () => setFieldErrors((prev) => ({ ...prev, [campo]: undefined })),
        delay,
      )
    }
  }

  function limparErros() {
    Object.values(timersRef.current).forEach(clearTimeout)
    timersRef.current = {}
    setFieldErrors({})
  }

  return { fieldErrors, setErroTemporario, limparErros }
}
