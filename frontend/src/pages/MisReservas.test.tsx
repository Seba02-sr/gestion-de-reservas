import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, waitFor, within } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import MisReservas from "./MisReservas"
import { renderConApp } from "../test/render"
import { ApiError } from "../api/client"
import type { ReservaResponse } from "../api/types"

vi.mock("../api/reservas", () => ({
  listarReservasDeHuesped: vi.fn(),
  pagarReserva: vi.fn(),
  cambiarEstado: vi.fn(),
  dejarReviewCliente: vi.fn(),
}))
import { cambiarEstado, dejarReviewCliente, listarReservasDeHuesped, pagarReserva } from "../api/reservas"

const base: ReservaResponse = {
  id: "r1", habitacionId: 10, hotelId: 1, idHuesped: "5", checkIn: "2099-12-10", checkOut: "2099-12-15",
  createdAt: "2099-01-01T00:00:00Z", precioNoche: 1000, precioTotal: 5000, totalPagado: 0, saldo: 5000,
  estado: "RESERVADA", pagos: [],
}
const pagada: ReservaResponse = {
  ...base, id: "r2", estado: "CONFIRMADA", totalPagado: 1000, saldo: 4000, createdAt: "2099-01-02T00:00:00Z",
  pagos: [{ method: "TARJETA", amount: { precio: 1000, moneda: "ARS" }, status: "COMPLETED" }],
}
const pasada: ReservaResponse = {
  ...pagada, id: "r3", checkIn: "2020-01-01", checkOut: "2020-01-05", createdAt: "2099-01-03T00:00:00Z",
}

function tarjeta(id: string) {
  // cada reserva es una Card; la ubicamos por el botón "Pagar" de su grupo
  return screen.getAllByRole("heading", { level: 2 }).find((h) => h.closest(".rarn-card")!.querySelector(`[data-id="${id}"]`) !== null)
}
void tarjeta

function botones(ordenReserva: number) {
  const cards = document.querySelectorAll<HTMLElement>(".rarn-reserva")
  const c = within(cards[ordenReserva])
  return {
    pagar: c.getByRole("button", { name: "Pagar" }),
    cancelar: c.getByRole("button", { name: /cancelar reserva/i }),
    opinar: c.getByRole("button", { name: /dejar opinión/i }),
  }
}

describe("Mis reservas", () => {
  beforeEach(() => {
    localStorage.setItem("user", JSON.stringify({ userId: 5, email: "ana@x.com", nombre: "Ana" }))
    vi.mocked(listarReservasDeHuesped).mockReset()
    vi.mocked(pagarReserva).mockReset()
    vi.mocked(cambiarEstado).mockReset()
    vi.mocked(dejarReviewCliente).mockReset()
  })

  it("lista las reservas del huésped logueado con estado, total, pagado y saldo", async () => {
    vi.mocked(listarReservasDeHuesped).mockResolvedValue([base])
    renderConApp(<MisReservas />, "/mis-reservas")
    expect(await screen.findByText("RESERVADA")).toBeInTheDocument()
    expect(vi.mocked(listarReservasDeHuesped).mock.calls[0][0]).toBe(5)
    expect(screen.getByText("Total")).toBeInTheDocument()
    expect(screen.getByText("Saldo")).toBeInTheDocument()
    expect(screen.getByText("10/12/2099 → 15/12/2099", { exact: false })).toBeInTheDocument()
  })

  it("habilita/deshabilita botones según el estado (más nueva primero)", async () => {
    // orden en pantalla: r3 (pasada), r2 (confirmada con pago), r1 (reservada sin pagos)
    vi.mocked(listarReservasDeHuesped).mockResolvedValue([base, pagada, pasada])
    renderConApp(<MisReservas />, "/mis-reservas")
    await screen.findAllByText(/CONFIRMADA|RESERVADA/)

    const r3 = botones(0)
    expect(r3.pagar).toBeEnabled()
    expect(r3.cancelar).toBeDisabled() // tiene pagos
    expect(r3.opinar).toBeEnabled() // ya pasó el check-out

    const r2 = botones(1)
    expect(r2.cancelar).toBeDisabled()
    expect(r2.cancelar).toHaveAttribute("title", expect.stringMatching(/pagos|confirmada/i))
    expect(r2.opinar).toBeDisabled() // check-out futuro

    const r1 = botones(2)
    expect(r1.pagar).toBeEnabled()
    expect(r1.cancelar).toBeEnabled() // sin pagos
    expect(r1.opinar).toBeDisabled()
  })

  it("paga con el monto elegido y recarga", async () => {
    vi.mocked(listarReservasDeHuesped).mockResolvedValue([base])
    vi.mocked(pagarReserva).mockResolvedValue({ ...base, estado: "CONFIRMADA", totalPagado: 2000, saldo: 3000 })
    const user = userEvent.setup()
    renderConApp(<MisReservas />, "/mis-reservas")
    await screen.findByText("RESERVADA")
    await user.click(botones(0).pagar)

    const dialogo = screen.getByRole("dialog", { name: /registrar pago/i })
    const monto = within(dialogo).getByLabelText(/monto a pagar/i)
    await user.clear(monto)
    await user.type(monto, "2000")
    await user.click(within(dialogo).getByRole("button", { name: /confirmar pago/i }))

    await waitFor(() => expect(pagarReserva).toHaveBeenCalledWith("r1", { method: "TARJETA", monto: 2000 }))
    expect(await screen.findByText("Pago registrado.")).toBeInTheDocument()
    expect(listarReservasDeHuesped).toHaveBeenCalledTimes(2)
  })

  it("no deja pagar más que el saldo", async () => {
    vi.mocked(listarReservasDeHuesped).mockResolvedValue([base])
    const user = userEvent.setup()
    renderConApp(<MisReservas />, "/mis-reservas")
    await screen.findByText("RESERVADA")
    await user.click(botones(0).pagar)
    const dialogo = screen.getByRole("dialog")
    const monto = within(dialogo).getByLabelText(/monto a pagar/i)
    await user.clear(monto)
    await user.type(monto, "999999")
    expect(within(dialogo).getByRole("button", { name: /confirmar pago/i })).toBeDisabled()
  })

  it("cancelar: si el backend responde 409 muestra su mensaje", async () => {
    vi.mocked(listarReservasDeHuesped).mockResolvedValue([base])
    vi.mocked(cambiarEstado).mockRejectedValue(new ApiError(409, "No se puede cancelar una reserva con pagos"))
    const user = userEvent.setup()
    renderConApp(<MisReservas />, "/mis-reservas")
    await screen.findByText("RESERVADA")
    await user.click(botones(0).cancelar)
    expect(await screen.findByRole("alert")).toHaveTextContent("No se puede cancelar una reserva con pagos")
    expect(cambiarEstado).toHaveBeenCalledWith("r1", "CANCELADA")
  })

  it("review: envía rating y comentario", async () => {
    vi.mocked(listarReservasDeHuesped).mockResolvedValue([pasada])
    vi.mocked(dejarReviewCliente).mockResolvedValue({ ...pasada, clientReview: { rating: 4 } })
    const user = userEvent.setup()
    renderConApp(<MisReservas />, "/mis-reservas")
    await screen.findByText("CONFIRMADA")
    await user.click(botones(0).opinar)
    const dialogo = screen.getByRole("dialog", { name: /dejar opinión/i })
    await user.selectOptions(within(dialogo).getByLabelText("Calificación"), "4")
    await user.type(within(dialogo).getByLabelText(/comentario/i), "Muy lindo")
    await user.click(within(dialogo).getByRole("button", { name: /enviar opinión/i }))
    await waitFor(() => expect(dejarReviewCliente).toHaveBeenCalledWith("r3", { rating: 4, comentario: "Muy lindo" }))
  })

  it("error de carga (503): mensaje amigable y reintento", async () => {
    vi.mocked(listarReservasDeHuesped).mockRejectedValue(new ApiError(503, "reservas-svc no responde"))
    renderConApp(<MisReservas />, "/mis-reservas")
    expect(await screen.findByRole("alert")).toHaveTextContent(/no está disponible/i)
    expect(screen.getByRole("button", { name: /reintentar/i })).toBeInTheDocument()
  })
})
