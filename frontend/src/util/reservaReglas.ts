import type { EstadoReserva, ReservaResponse } from "../api/types"
import { todayISO } from "./date"

/**
 * Reglas para HABILITAR botones. Son una ayuda de interfaz: el que decide siempre es el backend
 * (ReservaPolicy en reservas-svc), que responde 409 si la acción no corresponde.
 */
export interface Accion {
  habilitada: boolean
  /** Por qué está deshabilitada (se muestra como tooltip). */
  motivo?: string
}

const NO_ADMITEN_PAGO: EstadoReserva[] = ["CANCELADA", "FINALIZADA", "BLOQUEADA", "CERRADA"]
const ADMITEN_REVIEW: EstadoReserva[] = ["RESERVADA", "CONFIRMADA", "ADEUDADA", "FINALIZADA"]

export function puedePagar(r: ReservaResponse): Accion {
  if (NO_ADMITEN_PAGO.includes(r.estado)) return { habilitada: false, motivo: `No se puede pagar una reserva ${r.estado.toLowerCase()}` }
  if (r.saldo <= 0) return { habilitada: false, motivo: "La reserva ya está paga" }
  return { habilitada: true }
}

export function puedeCancelar(r: ReservaResponse): Accion {
  if (r.estado !== "RESERVADA" && r.estado !== "BLOQUEADA")
    return { habilitada: false, motivo: `No se puede cancelar una reserva ${r.estado.toLowerCase()}` }
  if ((r.pagos?.length ?? 0) > 0 || r.totalPagado > 0)
    return { habilitada: false, motivo: "No se puede cancelar una reserva que ya tiene pagos" }
  return { habilitada: true }
}

export function puedeDejarReview(r: ReservaResponse, hoy: string = todayISO()): Accion {
  if (!ADMITEN_REVIEW.includes(r.estado)) return { habilitada: false, motivo: `No se puede opinar sobre una reserva ${r.estado.toLowerCase()}` }
  if (r.clientReview) return { habilitada: false, motivo: "Ya dejaste tu opinión" }
  if (hoy < r.checkOut) return { habilitada: false, motivo: "Podés opinar a partir del día del check-out" }
  return { habilitada: true }
}
