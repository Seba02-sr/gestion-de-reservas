import { describe, expect, it } from "vitest"
import type { HabitacionResponse } from "../api/types"
import { nochesEntre, formatFecha } from "./date"
import { distanciaKm } from "./geo"
import { imagenDeHotel } from "./hotelImage"
import { ordenar } from "./orden"
import { destinoSeguro } from "./redirect"
import { validarRegistro } from "./validaciones"

const hab = (id: number, precio: number | null, estrellas: number): HabitacionResponse => ({
  habitacionId: id, numero: id, piso: 1, disponible: true, capacidad: 2, precioNoche: precio,
  amenities: [], idTipoHabitacion: 1, tipoHabitacion: "DOBLE",
  hotel: { id, nombre: `H${id}`, categoria: estrellas, domicilio: "x", latitud: 0, longitud: 0 },
})

describe("utilidades", () => {
  it("imagenDeHotel es determinística y siempre devuelve una de las 3 imágenes", () => {
    expect(imagenDeHotel(4)).toBe(imagenDeHotel(4))
    expect(imagenDeHotel(1)).toBe(imagenDeHotel(4))
    expect(new Set([1, 2, 3].map(imagenDeHotel)).size).toBe(3)
    expect(imagenDeHotel(-1)).toBeTruthy()
  })

  it("nochesEntre y formatFecha", () => {
    expect(nochesEntre("2026-12-10", "2026-12-15")).toBe(5)
    expect(nochesEntre("2026-12-15", "2026-12-10")).toBe(0)
    expect(nochesEntre(undefined, "2026-12-10")).toBe(0)
    expect(formatFecha("2026-12-10")).toBe("10/12/2026")
  })

  it("distanciaKm: Santa Fe - Rosario ≈ 140 km", () => {
    const d = distanciaKm(-31.6333, -60.7, -32.9442, -60.6505)
    expect(d).toBeGreaterThan(130)
    expect(d).toBeLessThan(160)
  })

  it("ordenar por precio, estrellas y distancia; sin tarifa al final", () => {
    const lista = [hab(1, 300, 3), hab(2, null, 5), hab(3, 100, 4)]
    expect(ordenar(lista, "precio-asc", () => 0).map((h) => h.habitacionId)).toEqual([3, 1, 2])
    expect(ordenar(lista, "precio-desc", () => 0).map((h) => h.habitacionId)).toEqual([1, 3, 2])
    expect(ordenar(lista, "estrellas", () => 0).map((h) => h.habitacionId)).toEqual([2, 3, 1])
    expect(ordenar(lista, "distancia", (h) => 10 - h.habitacionId).map((h) => h.habitacionId)).toEqual([3, 2, 1])
    expect(lista.map((h) => h.habitacionId)).toEqual([1, 2, 3]) // no muta
  })

  it("destinoSeguro rechaza redirecciones externas", () => {
    expect(destinoSeguro("/mis-reservas")).toBe("/mis-reservas")
    expect(destinoSeguro("https://malo.com")).toBe("/")
    expect(destinoSeguro("//malo.com")).toBe("/")
    expect(destinoSeguro(null)).toBe("/")
  })

  it("validarRegistro espeja las reglas del backend", () => {
    const ok = {
      nombre: "Ana Gómez", email: "ana@x.com", telefono: "342", dni: "30111222", username: "ana", password: "secreto",
      fechaNacimiento: "1990-01-01", numero: "5500 0000 0000 0004", nombreTitular: "Ana", fechaVencimiento: "12/28",
      cvc: "123", idBanco: "1",
    }
    expect(validarRegistro(ok, "2026-11-05")).toEqual({})
    const mal = validarRegistro({ ...ok, email: "x", numero: "12", fechaVencimiento: "13/28", cvc: "1", idBanco: "", fechaNacimiento: "2030-01-01" }, "2026-11-05")
    expect(Object.keys(mal).sort()).toEqual(["cvc", "email", "fechaNacimiento", "fechaVencimiento", "idBanco", "numero"])
  })
})
