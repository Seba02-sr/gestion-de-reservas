import { createContext, useContext } from "react"

/** Identidad del usuario logueado (lo que devuelven /auth/login y /auth/google). Sin token: ver docs de seguridad. */
export type AuthUser = {
  userId: number
  email: string
  nombre?: string | null
  givenName?: string | null
  familyName?: string | null
}

export type AuthContextValue = {
  user: AuthUser | null
  signIn: (u: AuthUser) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export const STORAGE_KEY = "user"

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth debe usarse dentro de <AuthProvider>")
  return ctx
}

export function leerUsuarioGuardado(): AuthUser | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const u = JSON.parse(raw) as AuthUser
    return typeof u?.userId === "number" ? u : null
  } catch {
    return null
  }
}
