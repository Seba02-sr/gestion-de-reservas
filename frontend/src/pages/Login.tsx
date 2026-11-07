import React, { useState } from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import GoogleLoginButton from "../components/auth/GoogleLoginButton"
import Alert from "../components/ui/Alert"
import Button from "../components/ui/Button"
import Card from "../components/ui/Card"
import { Field, Input } from "../components/ui/Field"
import { login } from "../api/auth"
import { mensajeAmigable } from "../api/client"
import { useAuth } from "../context/auth"
import { destinoSeguro } from "../util/redirect"

const Login: React.FC = () => {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { signIn } = useAuth()
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const googleHabilitado = !!import.meta.env.VITE_GOOGLE_CLIENT_ID

  const destino = destinoSeguro(params.get("redirect"))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      signIn(await login({ username: username.trim(), password }))
      navigate(destino, { replace: true })
    } catch (err) {
      setError(mensajeAmigable(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="rarn-page rarn-page--narrow">
      <Card>
        <h1 className="rarn-page__title">Iniciar sesión</h1>
        <p className="rarn-page__subtitle">Ingresá con tu usuario y contraseña</p>

        {error && <Alert kind="error">{error}</Alert>}

        <form onSubmit={handleSubmit} className="rarn-form">
          <Field label="Usuario" htmlFor="username">
            <Input id="username" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required />
          </Field>
          <Field label="Contraseña" htmlFor="password">
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </Field>
          <Button type="submit" className="rarn-btn--block" disabled={loading || !username || !password}>
            {loading ? "Ingresando..." : "Iniciar sesión"}
          </Button>
        </form>

        {googleHabilitado && (
          <>
            <div className="rarn-divider">
              <span>o</span>
            </div>
            <div className="flex justify-center">
              <GoogleLoginButton
                onSuccess={(data) => {
                  signIn(data)
                  navigate(destino, { replace: true })
                }}
                onError={setError}
              />
            </div>
          </>
        )}

        <p className="mt-6 text-center text-sm">
          ¿No tenés cuenta?{" "}
          <Link to={`/registro${params.get("redirect") ? `?redirect=${encodeURIComponent(destino)}` : ""}`} className="rarn-link">
            Registrate
          </Link>
        </p>
      </Card>
    </main>
  )
}

export default Login
