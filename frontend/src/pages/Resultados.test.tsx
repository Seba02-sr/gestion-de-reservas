import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { Route, Routes } from "react-router-dom"
import Resultados from "./Resultados"
import { renderConApp } from "../test/render"
import type { HabitacionResponse } from "../api/types"

vi.mock("../api/habitaciones", () => ({ buscarHabitaciones: vi.fn(), obtenerHabitacion: vi.fn() }))
import { buscarHabitaciones } from "../api/habitaciones"

const hab = (id: number, precio: number, estrellas: number, hotelId = 1, lat = -31.6, lng = -60.7): HabitacionResponse => ({
  habitacionId: id, numero: id, piso: 1, disponible: true, capacidad: 2, precioNoche: precio,
  amenities: ["WIFI"], idTipoHabitacion: 1, tipoHabitacion: "DOBLE",
  hotel: { id: hotelId, nombre: `Hotel ${hotelId}`, categoria: estrellas, domicilio: "Calle 1", latitud: lat, longitud: lng },
})

const app = () => (
  <Routes>
    <Route path="/buscar" element={<Resultados />} />
  </Routes>
)

describe("Resultados de búsqueda", () => {
  beforeEach(() => {
    vi.mocked(buscarHabitaciones).mockReset()
  })

  it("llama al backend con los parámetros de la URL (fechas, huéspedes, ubicación, hotel)", async () => {
    vi.mocked(buscarHabitaciones).mockResolvedValue([hab(1, 100, 3)])
    renderConApp(app(), "/buscar?checkIn=2099-12-10&checkOut=2099-12-15&cantidadHuespedes=2&lat=-31.6&lng=-60.7&radioKm=30&hotelId=4")
    await screen.findByText("1 habitación")
    const filtros = vi.mocked(buscarHabitaciones).mock.calls[0][0]!
    expect(filtros).toMatchObject({
      checkIn: "2099-12-10", checkOut: "2099-12-15", cantidadHuespedes: 2, lat: -31.6, lng: -60.7, radioKm: 30, hotelId: 4,
    })
  })

  it("muestra total estimado por las noches buscadas", async () => {
    vi.mocked(buscarHabitaciones).mockResolvedValue([hab(1, 10000, 3)])
    renderConApp(app(), "/buscar?checkIn=2099-12-10&checkOut=2099-12-15")
    expect(await screen.findByText(/Total 5 noches/)).toHaveTextContent("50.000")
  })

  it("aplica filtros (precio, estrellas, amenities) enviándolos al backend", async () => {
    vi.mocked(buscarHabitaciones).mockResolvedValue([])
    const user = userEvent.setup()
    renderConApp(app(), "/buscar")
    await screen.findByText("0 habitaciones")

    await user.type(screen.getByLabelText(/precio mínimo/i), "5000")
    await user.type(screen.getByLabelText(/precio máximo/i), "20000")
    await user.selectOptions(screen.getByLabelText(/estrellas/i), "4")
    await user.click(screen.getByLabelText("Wi-Fi"))
    await user.click(screen.getByLabelText("Spa"))
    await user.click(screen.getByRole("button", { name: /aplicar filtros/i }))

    await waitFor(() => {
      const ultimo = vi.mocked(buscarHabitaciones).mock.calls[vi.mocked(buscarHabitaciones).mock.calls.length - 1][0]!
      expect(ultimo).toMatchObject({ precioMin: 5000, precioMax: 20000, estrellas: 4, amenities: ["WIFI", "SPA"] })
    })
  })

  it("ordena por precio en el cliente", async () => {
    vi.mocked(buscarHabitaciones).mockResolvedValue([hab(1, 300, 3), hab(2, 100, 3, 2), hab(3, 200, 3, 3)])
    const user = userEvent.setup()
    renderConApp(app(), "/buscar")
    await screen.findByText("3 habitaciones")
    await user.selectOptions(screen.getByLabelText("Ordenar por"), "precio-asc")
    const titulos = screen.getAllByRole("heading", { level: 3 }).map((h) => h.textContent).filter((t) => t !== "Filtros")
    expect(titulos).toEqual(["Hotel 2", "Hotel 3", "Hotel 1"])
  })

  it("con hotelId muestra el encabezado del hotel y permite quitar el filtro", async () => {
    vi.mocked(buscarHabitaciones).mockResolvedValue([hab(1, 100, 3, 4)])
    const user = userEvent.setup()
    renderConApp(app(), "/buscar?hotelId=4")
    expect(await screen.findByRole("heading", { name: "Habitaciones de Hotel 4" })).toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: /quitar filtro de hotel/i }))
    expect(screen.getByTestId("location")).toHaveTextContent(/^\/buscar$/)
  })

  it("sin resultados y con error 503", async () => {
    vi.mocked(buscarHabitaciones).mockResolvedValueOnce([])
    const { unmount } = renderConApp(app(), "/buscar")
    expect(await screen.findByText(/no encontramos habitaciones/i)).toBeInTheDocument()
    unmount()

    const { ApiError } = await import("../api/client")
    vi.mocked(buscarHabitaciones).mockRejectedValueOnce(new ApiError(503, "reservas caído"))
    renderConApp(app(), "/buscar")
    expect(await screen.findByRole("alert")).toHaveTextContent(/no está disponible/i)
  })
})
