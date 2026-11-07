import React from "react"
import { Navigate, useLocation } from "react-router-dom"
import { useAuth } from "../../context/auth"

/** Solo deja pasar con sesión; si no, va a /login y vuelve acá después de loguearse. */
const RutaProtegida: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return <>{children}</>
}

export default RutaProtegida
