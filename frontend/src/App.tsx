import React from 'react'
import Header from './components/Header'
import Hero from "./components/Hero"

const App: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header /> {/* No necesitas envolverlo en div */}
      <main className="container mx-auto px-4 py-8">
        <Hero />
        {/* Aquí puedes agregar más componentes o contenido */}
      </main>
    </div>
  )
}

export default App