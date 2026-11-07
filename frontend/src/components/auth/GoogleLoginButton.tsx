import React, { useEffect, useRef } from "react"
import { loginConGoogle } from "../../api/auth"
import { mensajeAmigable } from "../../api/client"
import type { AuthUser } from "../../context/auth"

interface GoogleCredentialResponse {
  credential: string
}

interface GoogleIdentity {
  accounts: {
    id: {
      initialize: (cfg: {
        client_id: string
        callback: (r: GoogleCredentialResponse) => void
        auto_select?: boolean
      }) => void
      renderButton: (el: HTMLElement, opts: Record<string, string>) => void
    }
  }
}

declare global {
  interface Window {
    google?: GoogleIdentity
  }
}

type Props = {
  onSuccess?: (data: AuthUser) => void
  onError?: (message: string) => void
}

const GSI_SRC = "https://accounts.google.com/gsi/client"

/** Carga el script de Google Identity solo cuando hace falta (si hay clientId). */
function cargarGsi(): Promise<void> {
  if (window.google) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const existente = document.querySelector<HTMLScriptElement>(`script[src="${GSI_SRC}"]`)
    const s = existente ?? document.createElement("script")
    s.addEventListener("load", () => resolve(), { once: true })
    s.addEventListener("error", () => reject(new Error("No se pudo cargar Google")), { once: true })
    if (!existente) {
      s.src = GSI_SRC
      s.async = true
      document.head.appendChild(s)
    }
  })
}

/**
 * Botón "Continuar con Google". Es OPCIONAL: solo se renderiza si VITE_GOOGLE_CLIENT_ID está definido
 * al compilar; sin esa variable no muestra nada ni carga el script de Google.
 * El callback de sesión lo guarda el AuthContext (no se escribe localStorage acá).
 */
const GoogleLoginButton: React.FC<Props> = ({ onSuccess, onError }) => {
  const divRef = useRef<HTMLDivElement>(null)
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined
  const onSuccessRef = useRef(onSuccess)
  const onErrorRef = useRef(onError)
  onSuccessRef.current = onSuccess
  onErrorRef.current = onError

  useEffect(() => {
    if (!clientId) return
    let cancelado = false
    cargarGsi()
      .then(() => {
        if (cancelado || !window.google || !divRef.current) return
        window.google.accounts.id.initialize({
          client_id: clientId,
          auto_select: false,
          callback: async (response) => {
            try {
              onSuccessRef.current?.(await loginConGoogle(response.credential))
            } catch (e) {
              onErrorRef.current?.(mensajeAmigable(e))
            }
          },
        })
        window.google.accounts.id.renderButton(divRef.current, {
          theme: "outline",
          size: "large",
          shape: "pill",
          text: "continue_with",
          logo_alignment: "left",
          type: "standard",
        })
      })
      .catch((e: Error) => onErrorRef.current?.(e.message))
    return () => {
      cancelado = true
    }
  }, [clientId])

  if (!clientId) return null
  return <div ref={divRef} aria-label="Botón Google Login" />
}

export default GoogleLoginButton
