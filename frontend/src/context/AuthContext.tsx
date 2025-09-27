import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'

export type AuthUser = {
  userId: number
  email: string
  nombre?: string
  givenName?: string
  familyName?: string
}

type AuthContextValue = {
  user: AuthUser | null
  signIn: (u: AuthUser) => void
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null)

  useEffect(() => {
    const stored = localStorage.getItem('user')
    if (stored) {
      try {
        setUser(JSON.parse(stored))
      } catch {}
    }
    const onStorage = (e: StorageEvent) => {
      if (e.key === 'user') {
        if (e.newValue) setUser(JSON.parse(e.newValue))
        else setUser(null)
      }
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      signIn: (u) => {
        localStorage.setItem('user', JSON.stringify(u))
        setUser(u)
      },
      signOut: () => {
        localStorage.removeItem('user')
        setUser(null)
      },
    }),
    [user]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}

