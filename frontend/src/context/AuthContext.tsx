import React, { useEffect, useMemo, useState } from "react"
import { AuthContext, STORAGE_KEY, leerUsuarioGuardado, type AuthContextValue, type AuthUser } from "./auth"

/** Única escritora de localStorage['user']. */
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(() => leerUsuarioGuardado())

  useEffect(() => {
    // sincroniza entre pestañas
    const onStorage = (e: StorageEvent) => {
      if (e.key === STORAGE_KEY) setUser(leerUsuarioGuardado())
    }
    window.addEventListener("storage", onStorage)
    return () => window.removeEventListener("storage", onStorage)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      signIn: (u) => {
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(u))
        } catch {
          /* almacenamiento bloqueado: la sesión dura mientras la pestaña esté abierta */
        }
        setUser(u)
      },
      signOut: () => {
        try {
          localStorage.removeItem(STORAGE_KEY)
        } catch {
          /* idem */
        }
        setUser(null)
      },
    }),
    [user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
