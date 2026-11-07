/** Enum Amenity de gestion-svc (valor que entiende el backend -> etiqueta en pantalla). */
export const AMENITIES: Record<string, string> = {
  PILETA: "Pileta",
  SAUNA: "Sauna",
  GIMNASIO: "Gimnasio",
  RESTAURANTE: "Restaurante",
  BAR: "Bar",
  ESTACIONAMIENTO: "Estacionamiento",
  WIFI: "Wi-Fi",
  AIRE_ACONDICIONADO: "Aire acondicionado",
  CALENTADOR: "Calefacción",
  TV_CABLE: "TV por cable",
  SERVICIO_HABITACIONES: "Servicio a la habitación",
  LIMPIEZA_DIARIA: "Limpieza diaria",
  PISCINA_CUBIERTA: "Piscina cubierta",
  PISCINA_DESCUBIERTA: "Piscina descubierta",
  SPA: "Spa",
  SALA_JUEGOS: "Sala de juegos",
  SALA_REUNIONES: "Sala de reuniones",
  TRANSPORTE_AEROPUERTO: "Transporte al aeropuerto",
}

export const etiquetaAmenity = (a: string) => AMENITIES[a] ?? a
