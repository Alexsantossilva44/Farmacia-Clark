import { useState, useRef, useEffect } from 'react'

const ERROR_DELAY_MS = 3000

export function useErro() {
  const [error, setError] = useState('')
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(
    () => () => {
      if (timerRef.current) clearTimeout(timerRef.current)
    },
    [],
  )

  function showError(msg: string, afterDismiss?: () => void) {
    if (timerRef.current) clearTimeout(timerRef.current)
    setError(msg)
    timerRef.current = setTimeout(() => {
      setError('')
      afterDismiss?.()
    }, ERROR_DELAY_MS)
  }

  function clearError() {
    if (timerRef.current) clearTimeout(timerRef.current)
    setError('')
  }

  return { error, showError, clearError }
}
