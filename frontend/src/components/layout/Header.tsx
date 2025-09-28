import React, { useEffect, useRef, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { useAuth } from "../../context/AuthContext"

const Header: React.FC = () => {
  const { user, signOut } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (open && menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('click', onClick)
    return () => document.removeEventListener('click', onClick)
  }, [open])

  return (
    <header className="header">
      <div className="header__container">
        <div className="header__content">
          {/* Logo */}
          <div className="header__logo">
            <Link to="/" aria-label="Ir al inicio">
              <img src="/ARN-logo.svg" alt="Reservas ARN" className="h-8 w-auto" />
            </Link>
          </div>

          {/* Navigation */}
          <nav className="header__nav">
            <Link to="/" className="active">Inicio</Link>
            <a href="#hoteles">Hoteles</a>
            <a href="#mis-reservas">Mis Reservas</a>
            <a href="#propietarios">Propietarios</a>
          </nav>

          {/* Auth Actions */}
          <div className="header__actions flex gap-2 items-center relative" ref={menuRef}>
            {user ? (
              <>
                <button className="px-3 py-1.5 bg-white/70 rounded-full shadow-sm hover:shadow text-sm"
                        onClick={() => setOpen((v) => !v)}>
                  Hola, {(user.nombre || user.givenName || user.email).split(' ')[0]}
                </button>
                {open && (
                  <div className="absolute right-0 top-10 w-44 bg-white rounded-md shadow-lg border border-gray-100 z-50">
                    <Link to="#" className="block px-3 py-2 text-sm text-gray-700 hover:bg-gray-50" onClick={() => setOpen(false)}>
                      Perfil
                    </Link>
                    <button
                      className="w-full text-left px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                      onClick={() => {
                        setOpen(false)
                        signOut()
                        navigate('/')
                      }}
                    >
                      Cerrar sesión
                    </button>
                  </div>
                )}
              </>
            ) : (
              <>
                <Link to="/login" className="header__register-btn">Registrarse</Link>
                <Link to="/login" className="header__auth-btn">Iniciar sesión</Link>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  )
}

export default Header
