export interface Ciudad {
  id: string
  nombre: string
  lat: number
  lng: number
}

/** Destinos con coordenadas para la búsqueda por cercanía (lat/lng/radioKm del backend). */
export const CIUDADES: Ciudad[] = [
  { id: "santa-fe", nombre: "Santa Fe", lat: -31.6333, lng: -60.7 },
  { id: "rosario", nombre: "Rosario", lat: -32.9442, lng: -60.6505 },
  { id: "cordoba", nombre: "Córdoba", lat: -31.4201, lng: -64.1888 },
  { id: "buenos-aires", nombre: "Buenos Aires", lat: -34.6037, lng: -58.3816 },
  { id: "mendoza", nombre: "Mendoza", lat: -32.8908, lng: -68.8272 },
]

export const RADIO_DEFAULT_KM = 30
