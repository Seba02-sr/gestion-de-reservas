import React from "react"

const Header: React.FC = () => {
  return (
    <header className="header">
      <div className="header__container">
        <div className="header__content">
          {/* Logo */}
          <div className="header__logo">
            <img src="/src/assets/svg/ARN-logo.svg" alt="HotelReservas" className="h-8 w-auto" />
          </div>

          {/* Navigation */}
          <nav className="header__nav">
            <a href="#inicio" className="active">
              Inicio
            </a>
            <a href="#hoteles">Hoteles</a>
            <a href="#mis-reservas">Mis Reservas</a>
            <a href="#propietarios">Propietarios</a>
          </nav>

          {/* Auth Actions */}
          <div className="header__actions">
            <button className="header__register-btn">Registrarse</button>
            <button className="header__auth-btn">Iniciar Sesión</button>
          </div>
        </div>
      </div>
    </header>
  )
}

export default Header
