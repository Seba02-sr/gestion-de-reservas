import { get } from "./client"
import type { BusquedaHabitaciones, HabitacionResponse } from "./types"

/** GET /reservas/habitaciones/disponibles (reservas-svc, réplica de lectura). Todos los filtros son opcionales. */
export const buscarHabitaciones = (filtros: BusquedaHabitaciones = {}, signal?: AbortSignal) =>
  get<HabitacionResponse[]>(
    "/reservas/habitaciones/disponibles",
    { ...filtros, amenities: filtros.amenities?.length ? filtros.amenities : undefined },
    signal
  )

export const obtenerHabitacion = (id: number | string, signal?: AbortSignal) =>
  get<HabitacionResponse>(`/reservas/habitaciones/${id}`, undefined, signal)
