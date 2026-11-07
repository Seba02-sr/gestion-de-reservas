import { describe, expect, it } from "vitest"
import type { EstadoReserva, ReservaResponse } from "../api/types"
import { puedeCancelar, puedeDejarReview, puedePagar } from "./reservaReglas"

const reserva = (o: Partial<ReservaResponse> = {}): ReservaResponse => ({
  id: "r1",
  habitacionId: 1,
  hotelId: 1,
  idHuesped: "5",
  checkIn: "2026-12-10",
  checkOut: "2026-12-15",
  createdAt: "2026-11-01T10:00:00Z",
  precioNoche: 1000,
  precioTotal: 5000,
  totalPagado: 0,
  saldo: 5000,
  estado: "RESERVADA",
  pagos: [],
  ...o,
})

describe("reglas de habilitación de acciones (espejo de ReservaPolicy)", () => {
  it("pagar: habilitado con saldo y estado válido", () => {
    expect(puedePagar(reserva()).habilitada).toBe(true)
    expect(puedePagar(reserva({ estado: "CONFIRMADA", totalPagado: 1000, saldo: 4000 })).habilitada).toBe(true)
    expect(puedePagar(reserva({ estado: "ADEUDADA" })).habilitada).toBe(true)
  })

  it.each<EstadoReserva>(["CANCELADA", "FINALIZADA", "BLOQUEADA", "CERRADA"])("pagar: deshabilitado en %s", (estado) => {
    const a = puedePagar(reserva({ estado }))
    expect(a.habilitada).toBe(false)
    expect(a.motivo).toBeTruthy()
  })

  it("pagar: deshabilitado sin saldo", () => {
    expect(puedePagar(reserva({ estado: "CONFIRMADA", saldo: 0, totalPagado: 5000 })).habilitada).toBe(false)
  })

  it("cancelar: solo RESERVADA/BLOQUEADA y sin pagos", () => {
    expect(puedeCancelar(reserva()).habilitada).toBe(true)
    expect(puedeCancelar(reserva({ estado: "BLOQUEADA" })).habilitada).toBe(true)
    expect(puedeCancelar(reserva({ estado: "CONFIRMADA" })).habilitada).toBe(false)
    const conPago = puedeCancelar(
      reserva({ totalPagado: 1000, saldo: 4000, pagos: [{ method: "TARJETA", amount: { precio: 1000, moneda: "ARS" }, status: "COMPLETED" }] })
    )
    expect(conPago.habilitada).toBe(false)
    expect(conPago.motivo).toMatch(/pagos/)
  })

  it("review: solo desde el día del check-out, una vez y en estados válidos", () => {
    expect(puedeDejarReview(reserva({ estado: "CONFIRMADA" }), "2026-12-14").habilitada).toBe(false)
    expect(puedeDejarReview(reserva({ estado: "CONFIRMADA" }), "2026-12-15").habilitada).toBe(true)
    expect(puedeDejarReview(reserva({ estado: "CONFIRMADA" }), "2027-01-15").habilitada).toBe(true)
    expect(puedeDejarReview(reserva({ estado: "CONFIRMADA", clientReview: { rating: 5 } }), "2027-01-15").habilitada).toBe(false)
    expect(puedeDejarReview(reserva({ estado: "CANCELADA" }), "2027-01-15").habilitada).toBe(false)
  })
})
