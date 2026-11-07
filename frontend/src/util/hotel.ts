// Modelo de presentación de un hotel destacado (armado desde HotelResponse + habitaciones)
export interface Hotel {
  id: number
  nombre: string
  ubicacion: string
  /** Categoría (estrellas) del hotel */
  rating: number
  /** Precio por noche más bajo entre sus habitaciones con tarifa; undefined si no hay */
  precio?: number
  imagen: string
}
