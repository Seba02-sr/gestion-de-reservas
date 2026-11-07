import React from 'react'
import Hero from "../components/hero/Hero"
import ExploraArgentina from '../components/explora/ExploraArgentina'
import HotelesDestacados from '../components/hoteles_destacados/HotelesDestacados'
import Benefits from '../components/benefits/Benefits'
import Footer from '../components/layout/Footer'

const Home: React.FC = () => {
  return (
    <main className="mx-auto">
      <Hero />
      <ExploraArgentina />
      <HotelesDestacados />
      <Benefits />
      <Footer />
    </main>
  )
}

export default Home

