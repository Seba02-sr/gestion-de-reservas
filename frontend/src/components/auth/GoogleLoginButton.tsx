import React, { useEffect, useRef } from 'react'

declare global {
  interface Window {
    google?: any
  }
}

type Props = {
  onSuccess?: (data: any) => void
  onError?: (message: string) => void
}

const GoogleLoginButton: React.FC<Props> = ({ onSuccess, onError }) => {
  const divRef = useRef<HTMLDivElement>(null)
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string

  useEffect(() => {
    if (!clientId) return
    const interval = setInterval(() => {
      if (window.google && divRef.current) {
        clearInterval(interval)
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: async (response: any) => {
            try {
              const res = await fetch('http://localhost:8081/auth/google', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ credential: response.credential }),
              })
              if (!res.ok) throw new Error('Fallo la verificación del token')
              const data = await res.json()
              localStorage.setItem('user', JSON.stringify(data))
              onSuccess?.(data)
            } catch (e: any) {
              console.error(e)
              onError?.(e.message)
            }
          },
          auto_select: false,
        })
        window.google.accounts.id.renderButton(divRef.current, {
          theme: 'outline',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          logo_alignment: 'left',
          type: 'standard',
        })
      }
    }, 100)
    return () => clearInterval(interval)
  }, [clientId])

  if (!clientId) {
    return (
      <button
        className="header__auth-btn opacity-70 cursor-not-allowed"
        title="Configura VITE_GOOGLE_CLIENT_ID para habilitar Google Login"
        disabled
      >
        Iniciar con Google
      </button>
    )
  }

  return <div ref={divRef} aria-label="Botón Google Login" />
}

export default GoogleLoginButton
