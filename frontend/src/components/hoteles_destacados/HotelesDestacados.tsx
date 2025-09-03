// src/components/hoteles/HotelesDestacados.tsx
"use client"

import { HotelCard } from "./HotelCard"
import type { Hotel } from "../../util/hotel"


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
    <section className="py-16 px-4 bg-white">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-12">
          <h2 className="rarn-typography-h2 mb-4">Hoteles Destacados</h2>
          <p className="rarn-typography-p text-gray-600">
            Descubre los mejores alojamientos seleccionados para ti
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 mb-12">
          {hotelesData.map((hotel) => (
            <HotelCard key={hotel.id} hotel={hotel} onVerDetalles={handleVerDetalles} />
          ))}
        </div>

        <div className="text-center">
          <button
            onClick={handleVerTodos}
            className="bg-rarn-dark text-white py-3 px-8 rounded-md rarn-typography-span font-medium hover:bg-opacity-90 transition-colors duration-200"
          >
            Ver todos los hoteles
          </button>
        </div>
      </div>
    </section>
  )
}
