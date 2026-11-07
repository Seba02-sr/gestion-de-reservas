import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import HotelesDestacados from "./HotelesDestacados"
import { renderConApp } from "../../test/render"
import { ApiError } from "../../api/client"
import type { HabitacionResponse, HotelResponse } from "../../api/types"

vi.mock("../../api/hoteles", () => ({ listarHotelesAbiertos: vi.fn() }))
vi.mock("../../api/habitaciones", () => ({ buscarHabitaciones: vi.fn() }))
import { listarHotelesAbiertos } from "../../api/hoteles"
import { buscarHabitaciones } from "../../api/habitaciones"

const hotel = (id: number, nombre: string): HotelResponse => ({
  id, nombre, domicilio: `Calle ${id}`, latitud: 0, longitud: 0, categoria: 4, cerrado: false,
})
const hab = (id: number, hotelId: number, precio: number | null): HabitacionResponse => ({
  habitacionId: id, numero: id, piso: 1, disponible: true, capacidad: 2, precioNoche: precio, amenities: [],
  idTipoHabitacion: 1, tipoHabitacion: "DOBLE",
  hotel: { id: hotelId, nombre: "x", categoria: 4, domicilio: "x", latitud: 0, longitud: 0 },
})

describe("Hoteles destacados (datos reales)", () => {
  beforeEach(() => {
    vi.mocked(listarHotelesAbiertos).mockReset()
    vi.mocked(buscarHabitaciones).mockReset()
  })

  it("muestra los hoteles de la API con el precio mínimo 'desde' y navega con hotelId", async () => {
    vi.mocked(listarHotelesAbiertos).mockResolvedValue([hotel(4, "Hotel Castelar"), hotel(5, "Sin tarifas")])
    vi.mocked(buscarHabitaciones).mockResolvedValue([hab(1, 4, 20000), hab(2, 4, 15000), hab(3, 4, null)])
    const user = userEvent.setup()
    renderConApp(<HotelesDestacados />)

    expect(await screen.findByRole("heading", { name: "Hotel Castelar" })).toBeInTheDocument()
    expect(screen.queryByText(/Alvear|Cuba|Llao Llao/)).not.toBeInTheDocument()
    expect(screen.getByText(/15\.000/)).toBeInTheDocument() // mínimo, no 20.000
    expect(screen.getByText("Consultá disponibilidad")).toBeInTheDocument() // hotel 5 sin habitaciones

    await user.click(screen.getAllByRole("button", { name: "Ver detalles" })[0])
    expect(screen.getByTestId("location")).toHaveTextContent("/buscar?hotelId=4")
  })

  it("si falla la consulta de precios igual muestra los hoteles", async () => {
    vi.mocked(listarHotelesAbiertos).mockResolvedValue([hotel(4, "Hotel Castelar")])
    vi.mocked(buscarHabitaciones).mockRejectedValue(new ApiError(503, "reservas caído"))
    renderConApp(<HotelesDestacados />)
    expect(await screen.findByRole("heading", { name: "Hotel Castelar" })).toBeInTheDocument()
  })

  it("si falla gestion-svc muestra un error amigable", async () => {
    vi.mocked(listarHotelesAbiertos).mockRejectedValue(new ApiError(503, "gestion caído"))
    renderConApp(<HotelesDestacados />)
    expect(await screen.findByRole("alert")).toHaveTextContent(/no está disponible/i)
  })

  it("sin hoteles muestra un mensaje", async () => {
    vi.mocked(listarHotelesAbiertos).mockResolvedValue([])
    vi.mocked(buscarHabitaciones).mockResolvedValue([])
    renderConApp(<HotelesDestacados />)
    expect(await screen.findByText(/todavía no hay hoteles/i)).toBeInTheDocument()
  })
})
