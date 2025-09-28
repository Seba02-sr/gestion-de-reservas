import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import GoogleLoginButton from '../components/auth/GoogleLoginButton'
import { useAuth } from '../context/AuthContext'

const Login: React.FC = () => {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { signIn } = useAuth()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('El login con usuario/contraseña estará disponible pronto.')
  }

  return (
    <div className="min-h-[70vh] flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md bg-white shadow-lg rounded-xl p-6">
        <h1 className="text-2xl font-semibold text-gray-800 text-center mb-2">Iniciar sesión</h1>
        <p className="text-sm text-gray-500 text-center mb-6">Accedé con tu cuenta o con Google</p>

        {error && (
          <div className="mb-4 rounded-md bg-yellow-50 p-3 text-yellow-800 text-sm border border-yellow-200">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="tucorreo@ejemplo.com"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              placeholder="••••••••"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full bg-gray-300 text-gray-600 font-medium py-2 rounded-md cursor-not-allowed"
            disabled
            title="Pronto disponible"
          >
            Iniciar sesión
          </button>
        </form>

        <div className="my-6 flex items-center">
          <div className="flex-1 h-px bg-gray-200" />
          <span className="px-3 text-xs uppercase text-gray-400">o</span>
          <div className="flex-1 h-px bg-gray-200" />
        </div>

        <div className="flex justify-center">
          <GoogleLoginButton
            onSuccess={(data) => {
              signIn(data)
              navigate('/')
            }}
            onError={(msg) => setError(msg)}
          />
        </div>

        <p className="mt-6 text-center text-sm text-gray-600">
          ¿No tenés cuenta?{' '}
          <span className="text-indigo-600 hover:underline cursor-pointer" title="Pronto disponible">
            Registrate
          </span>
        </p>
      </div>
    </div>
  )
}

export default Login
