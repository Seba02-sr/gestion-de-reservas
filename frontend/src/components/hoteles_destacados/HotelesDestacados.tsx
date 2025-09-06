// src/components/hoteles/HotelesDestacados.tsx
"use client"

import { HotelCard } from "./HotelCard"
import type { Hotel } from "../../util/hotel"
import SectionHeader from "../ui/SectionHeader"
import Section from "../ui/Section"


import hotel1 from "@/assets/images/hotel1.webp"
import hotel2 from "@/assets/images/hotel2.webp"
import hotel3 from "@/assets/images/hotel3.webp"

export const hotelesData: Hotel[] = [
  {
    id: 1,
    nombre: "Alvear Palace Hotel",
    ubicacion: "Recoleta, Buenos Aires",
    rating: 4.8,
    precio: 45000, // aprox. ARS por noche
    imagen: hotel1,
  },
  {
    id: 2,
    nombre: "Hotel Nacional de Cuba",
    ubicacion: "Vedado, La Habana",
    rating: 4.7,
    precio: 38000, // aprox. ARS por noche
    imagen: hotel2,
  },
  {
    id: 3,
    nombre: "Llao Llao Resort, Golf & Spa",
    ubicacion: "Bariloche, Río Negro",
    rating: 4.9,
    precio: 52000, // aprox. ARS por noche
    imagen: hotel3,
  },
]

export default function HotelesDestacados() {
  const handleVerDetalles = (id: number) => {
    console.log(`[v0] Ver detalles del hotel ID: ${id}`)
    // TODO: navegar a /hoteles/:id
  }

  const handleVerTodos = () => {
    console.log("[v0] Ver todos los hoteles")
    // TODO: navegar a /hoteles
  }

  return (
    <Section className="hoteles" containerClassName="hoteles__container">
        <SectionHeader
          className="hoteles__header"
          titleClassName="hoteles__title rarn-typography-h2"
          subtitleClassName="hoteles__subtitle rarn-typography-p"
          title="Hoteles Destacados"
          subtitle="Descubre los mejores alojamientos seleccionados para ti"
        />

        <div className="hoteles__grid">
          {hotelesData.map((hotel) => (
            <HotelCard key={hotel.id} hotel={hotel} onVerDetalles={handleVerDetalles} />
          ))}
        </div>

        <div className="hoteles__actions">
          <button onClick={handleVerTodos} className="rarn-btn rarn-btn--primary rarn-btn--lg">
            Ver todos los hoteles
          </button>
        </div>
    </Section>
  )
}
