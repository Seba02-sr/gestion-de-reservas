import { get, patch, post, put } from "./client"
import type { EstadoReserva, PagoRequest, ReservaRequest, ReservaResponse, ReviewRequest } from "./types"

// UserController y ReservaController conservan su prefijo: por el gateway queda /reservas/reservas/...

export const crearReserva = (req: ReservaRequest) => post<ReservaResponse>("/reservas/reservas", req)

export const listarReservasDeHuesped = (idHuesped: number, signal?: AbortSignal) =>
  get<ReservaResponse[]>("/reservas/reservas", { idHuesped }, signal)

export const obtenerReserva = (id: string) => get<ReservaResponse>(`/reservas/reservas/${id}`)

export const pagarReserva = (id: string, req: PagoRequest) =>
  post<ReservaResponse>(`/reservas/reservas/${id}/pagos`, req)

export const cambiarEstado = (id: string, estado: EstadoReserva) =>
  patch<ReservaResponse>(`/reservas/reservas/${id}/estado`, { estado })

export const dejarReviewCliente = (id: string, req: ReviewRequest) =>
  put<ReservaResponse>(`/reservas/reservas/${id}/review-cliente`, req)
