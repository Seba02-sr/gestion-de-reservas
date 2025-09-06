import React from 'react'
import Header from './components/layout/Header'
import Hero from "./components/hero/Hero"
import ExploraArgentina from './components/explora/ExploraArgentina'
import HotelesDestacados from './components/hoteles_destacados/HotelesDestacados'
import Benefits from './components/benefits/Benefits'
import Footer from './components/layout/Footer'

const App: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header /> {/* No necesitas envolverlo en div */}
      <main className="mx-auto">
        <Hero />
        {/* Aquí puedes agregar más componentes o contenido */}
        <ExploraArgentina />
        {/* Aquí puedes agregar más componentes o contenido */}
        <HotelesDestacados />
        {/* Aquí puedes agregar más componentes o contenido */}
        <Benefits />

        <Footer />
      </main>
    </div>
  )
}

export default App