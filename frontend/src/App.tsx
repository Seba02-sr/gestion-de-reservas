import React from 'react'
import Header from './components/layout/Header'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Home from './pages/Home'
import Login from './pages/Login'
import Registro from './pages/Registro'
import Resultados from './pages/Resultados'
import DetalleHabitacion from './pages/DetalleHabitacion'
import MisReservas from './pages/MisReservas'
import RutaProtegida from './components/auth/RutaProtegida'
import { AuthProvider } from './context/AuthContext'

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="min-h-screen bg-gray-50">
          <Header />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/registro" element={<Registro />} />
            <Route path="/buscar" element={<Resultados />} />
            <Route path="/habitaciones/:id" element={<DetalleHabitacion />} />
            <Route
              path="/mis-reservas"
              element={
                <RutaProtegida>
                  <MisReservas />
                </RutaProtegida>
              }
            />
          </Routes>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
