import { useCallback, useEffect, useRef, useState } from "react"
import { mensajeAmigable } from "../api/client"

export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string | null
  reload: () => void
}

/**
 * Ejecuta `fn` al montar y cada vez que cambia `deps`. Cancela el pedido anterior (AbortSignal)
 * y expone un mensaje de error ya amigable.
 */
export function useAsync<T>(fn: (signal: AbortSignal) => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [tick, setTick] = useState(0)
  const fnRef = useRef(fn)
  fnRef.current = fn

  useEffect(() => {
    const ctrl = new AbortController()
    setLoading(true)
    setError(null)
    fnRef
      .current(ctrl.signal)
      .then((d) => {
        if (!ctrl.signal.aborted) setData(d)
      })
      .catch((e) => {
        if (ctrl.signal.aborted) return
        setError(mensajeAmigable(e))
        setData(null)
      })
      .finally(() => {
        if (!ctrl.signal.aborted) setLoading(false)
      })
    return () => ctrl.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick])

  const reload = useCallback(() => setTick((t) => t + 1), [])
  return { data, loading, error, reload }
}
