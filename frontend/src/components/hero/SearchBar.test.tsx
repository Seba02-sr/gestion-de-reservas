import { describe, expect, it } from "vitest"
import { screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import SearchBar from "./SearchBar"
import { renderConApp } from "../../test/render"

describe("SearchBar", () => {
  it("navega a /buscar con ciudad (lat/lng/radio), fechas y huéspedes", async () => {
    const user = userEvent.setup()
    renderConApp(<SearchBar />)

    await user.selectOptions(screen.getByLabelText("Destino"), "santa-fe")
    await user.type(screen.getByLabelText("Check-in"), "2099-12-10")
    await user.type(screen.getByLabelText("Check-out"), "2099-12-15")
    await user.selectOptions(screen.getByLabelText("Huéspedes"), "2")
    await user.click(screen.getByRole("button", { name: /buscar hoteles/i }))

    const url = new URL("http://x" + screen.getByTestId("location").textContent)
    expect(url.pathname).toBe("/buscar")
    expect(url.searchParams.get("lat")).toBe("-31.6333")
    expect(url.searchParams.get("lng")).toBe("-60.7")
    expect(url.searchParams.get("radioKm")).toBe("30")
    expect(url.searchParams.get("checkIn")).toBe("2099-12-10")
    expect(url.searchParams.get("checkOut")).toBe("2099-12-15")
    expect(url.searchParams.get("cantidadHuespedes")).toBe("2")
  })

  it("'Cualquier lugar' y sin fechas: no envía ubicación ni fechas", async () => {
    const user = userEvent.setup()
    renderConApp(<SearchBar />)
    await user.click(screen.getByRole("button", { name: /buscar hoteles/i }))
    const url = new URL("http://x" + screen.getByTestId("location").textContent)
    expect(url.pathname).toBe("/buscar")
    expect(url.searchParams.has("lat")).toBe(false)
    expect(url.searchParams.has("checkIn")).toBe(false)
  })

  it("no deja buscar con una sola fecha", async () => {
    const user = userEvent.setup()
    renderConApp(<SearchBar />)
    await user.type(screen.getByLabelText("Check-in"), "2099-12-10")
    expect(screen.getByText(/indicá también el check-out/i)).toBeInTheDocument()
    expect(screen.getByRole("button", { name: /buscar hoteles/i })).toBeDisabled()
  })

  it("si se niega la ubicación muestra un mensaje y permite elegir ciudad", async () => {
    const user = userEvent.setup()
    Object.defineProperty(navigator, "geolocation", {
      configurable: true,
      value: { getCurrentPosition: (_ok: unknown, fail: () => void) => fail() },
    })
    renderConApp(<SearchBar />)
    await user.selectOptions(screen.getByLabelText("Destino"), "ubicacion")
    expect(await screen.findByText(/no pudimos obtener tu ubicación/i)).toBeInTheDocument()
  })

  it("con 'Cerca de mi ubicación' usa las coordenadas del navegador", async () => {
    const user = userEvent.setup()
    Object.defineProperty(navigator, "geolocation", {
      configurable: true,
      value: {
        getCurrentPosition: (ok: (p: { coords: { latitude: number; longitude: number } }) => void) =>
          ok({ coords: { latitude: -31.5, longitude: -60.6 } }),
      },
    })
    renderConApp(<SearchBar />)
    await user.selectOptions(screen.getByLabelText("Destino"), "ubicacion")
    await user.click(screen.getByRole("button", { name: /buscar hoteles/i }))
    const url = new URL("http://x" + screen.getByTestId("location").textContent)
    expect(url.searchParams.get("lat")).toBe("-31.5")
    expect(url.searchParams.get("lng")).toBe("-60.6")
  })
})
