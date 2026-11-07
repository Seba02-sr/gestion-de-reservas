import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { Route, Routes } from "react-router-dom"
import DetalleHabitacion from "./DetalleHabitacion"
import { renderConApp } from "../test/render"
import { ApiError } from "../api/client"
import type { HabitacionResponse } from "../api/types"

vi.mock("../api/habitaciones", () => ({ obtenerHabitacion: vi.fn(), buscarHabitaciones: vi.fn() }))
vi.mock("../api/reservas", () => ({ crearReserva: vi.fn() }))
import { obtenerHabitacion } from "../api/habitaciones"
import { crearReserva } from "../api/reservas"

const habitacion = (precio: number | null = 10000): HabitacionResponse => ({
  habitacionId: 7, numero: 101, piso: 1, disponible: true, capacidad: 3, precioNoche: precio,
  amenities: ["WIFI", "PILETA"], idTipoHabitacion: 2, tipoHabitacion: "TRIPLE",
  hotel: { id: 4, nombre: "Hotel Castelar", categoria: 4, domicilio: "Bv. Pellegrini 2500", latitud: -31.6, longitud: -60.7 },
})

const app = () => (
  <Routes>
    <Route path="/habitaciones/:id" element={<DetalleHabitacion />} />
    <Route path="/mis-reservas" element={<p>pantalla mis reservas</p>} />
  </Routes>
)

const RUTA = "/habitaciones/7?checkIn=2099-12-10&checkOut=2099-12-13&cantidadHuespedes=2"

describe("Detalle de habitación", () => {
  beforeEach(() => {
    vi.mocked(obtenerHabitacion).mockReset()
    vi.mocked(crearReserva).mockReset()
  })

  it("muestra hotel, tipo, capacidad, amenities, precio y total estimado", async () => {
    vi.mocked(obtenerHabitacion).mockResolvedValue(habitacion())
    renderConApp(app(), RUTA)
    expect(await screen.findByRole("heading", { name: "Hotel Castelar" })).toBeInTheDocument()
    expect(screen.getByText(/Tipo: TRIPLE/)).toBeInTheDocument()
    expect(screen.getByText(/Capacidad: 3/)).toBeInTheDocument()
    expect(screen.getByText("Wi-Fi")).toBeInTheDocument()
    expect(screen.getByText("Pileta")).toBeInTheDocument()
    expect(screen.getByText(/× 3 noches/)).toBeInTheDocument()
    expect(screen.getByText("Total estimado").parentElement).toHaveTextContent("30.000")
  })

  it("sin sesión pide iniciar sesión (redirige a /login con redirect)", async () => {
    vi.mocked(obtenerHabitacion).mockResolvedValue(habitacion())
    const user = userEvent.setup()
    renderConApp(app(), RUTA)
    await user.click(await screen.findByRole("button", { name: /iniciar sesión para reservar/i }))
    expect(screen.getByTestId("location")).toHaveTextContent(/^\/login\?redirect=/)
    expect(crearReserva).not.toHaveBeenCalled()
  })

  it("con sesión reserva con el userId y va a Mis reservas", async () => {
    localStorage.setItem("user", JSON.stringify({ userId: 5, email: "ana@x.com" }))
    vi.mocked(obtenerHabitacion).mockResolvedValue(habitacion())
    vi.mocked(crearReserva).mockResolvedValue({} as never)
    const user = userEvent.setup()
    renderConApp(app(), RUTA)
    await user.click(await screen.findByRole("button", { name: /^reservar$/i }))
    await waitFor(() =>
      expect(crearReserva).toHaveBeenCalledWith({ habitacionId: 7, idHuesped: 5, checkIn: "2099-12-10", checkOut: "2099-12-13" })
    )
    expect(await screen.findByText("pantalla mis reservas")).toBeInTheDocument()
  })

  it.each([
    [409, "La habitación no está disponible entre esas fechas", /no está disponible entre esas fechas/],
    [409, "La habitación no tiene tarifa vigente", /no tiene tarifa vigente/],
    [404, "No existe el huésped 5", /No existe el huésped 5/],
    [503, "user-svc no responde", /no está disponible, probá en unos segundos/],
  ])("reservar con %i muestra el mensaje y no navega", async (status, mensaje, esperado) => {
    localStorage.setItem("user", JSON.stringify({ userId: 5, email: "ana@x.com" }))
    vi.mocked(obtenerHabitacion).mockResolvedValue(habitacion())
    vi.mocked(crearReserva).mockRejectedValue(new ApiError(status, mensaje))
    const user = userEvent.setup()
    renderConApp(app(), RUTA)
    await user.click(await screen.findByRole("button", { name: /^reservar$/i }))
    expect(await screen.findByRole("alert")).toHaveTextContent(esperado)
    expect(screen.queryByText("pantalla mis reservas")).not.toBeInTheDocument()
  })

  it("sin tarifa vigente deshabilita Reservar", async () => {
    vi.mocked(obtenerHabitacion).mockResolvedValue(habitacion(null))
    renderConApp(app(), RUTA)
    expect(await screen.findByRole("button", { name: /iniciar sesión para reservar/i })).toBeDisabled()
  })

  it("habitación inexistente (404)", async () => {
    vi.mocked(obtenerHabitacion).mockRejectedValue(new ApiError(404, "No existe la habitación 7"))
    renderConApp(app(), RUTA)
    expect(await screen.findByRole("alert")).toHaveTextContent("No existe la habitación 7")
  })
})
