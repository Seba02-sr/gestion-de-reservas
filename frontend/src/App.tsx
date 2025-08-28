import React from 'react'
import Header from './components/Header'

const App: React.FC = () => {
  return (
    <div>
      <Header />
      <main>
        <h1 className="text-2xl mt-4">Bienvenido a HotelReservas</h1>
      </main>
    </div>
  )
}

export default App
