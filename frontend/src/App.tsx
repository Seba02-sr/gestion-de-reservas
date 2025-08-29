import React from 'react'
import Header from './components/Header'
import Hero from "./components/Hero"

const App: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header /> {/* No necesitas envolverlo en div */}
      <main className="mx-auto">
        <Hero />
        {/* Aquí puedes agregar más componentes o contenido */}
      </main>
    </div>
  )
}

export default App