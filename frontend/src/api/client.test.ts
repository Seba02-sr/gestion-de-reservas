import { describe, expect, it, vi } from "vitest"
import { ApiError, MENSAJE_503, MENSAJE_RED, apiUrl, get, mensajeAmigable, post } from "./client"

function respuesta(status: number, body?: unknown): Response {
  return new Response(body === undefined ? "" : JSON.stringify(body), { status })
}

/** Espera que la promesa falle y devuelve el error tipado. */
async function falla(p: Promise<unknown>): Promise<ApiError> {
  try {
    await p
  } catch (e) {
    return e as ApiError
  }
  throw new Error("se esperaba un error")
}

describe("cliente de API", () => {
  it("usa VITE_API_URL (sin barra final) y localhost:8080 por defecto", () => {
    vi.stubEnv("VITE_API_URL", "/api/")
    expect(apiUrl()).toBe("/api")
    vi.stubEnv("VITE_API_URL", undefined as unknown as string)
    expect(apiUrl()).toBe("http://localhost:8080")
  })

  it("arma la query ignorando vacíos y repitiendo arrays", async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(200, []))
    vi.stubGlobal("fetch", fetchMock)
    await get("/reservas/habitaciones/disponibles", { lat: -31.6, hotelId: undefined, vacio: "", amenities: ["WIFI", "SPA"] })
    expect(fetchMock.mock.calls[0][0]).toBe(
      "http://localhost:8080/reservas/habitaciones/disponibles?lat=-31.6&amenities=WIFI&amenities=SPA"
    )
  })

  it("envía JSON en el POST y devuelve el cuerpo", async () => {
    const fetchMock = vi.fn().mockResolvedValue(respuesta(201, { id: 7 }))
    vi.stubGlobal("fetch", fetchMock)
    const r = await post<{ id: number }>("/users/users/huesped", { nombre: "Ana" })
    expect(r.id).toBe(7)
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.method).toBe("POST")
    expect(init.body).toBe('{"nombre":"Ana"}')
  })

  it.each([
    [400, "nombre: no puede estar vacío"],
    [404, "No existe el huésped 9"],
    [409, "La habitación no está disponible en esas fechas"],
  ])("%i: conserva el mensaje del backend", async (status, message) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(respuesta(status, { message, path: "uri=/x", status })))
    const err = await falla(get("/x"))
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(status)
    expect(mensajeAmigable(err)).toBe(message)
  })

  it("401: mensaje de credenciales", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(respuesta(401, { message: "Usuario o contraseña incorrectos" })))
    const err = await falla(post("/users/auth/login", {}))
    expect(mensajeAmigable(err)).toBe("Usuario o contraseña incorrectos")
  })

  it("503 se traduce a un mensaje amigable", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(respuesta(503, { message: "user-svc no responde: Connection refused", status: 503 }))
    )
    const err = await falla(get("/x"))
    expect(err.status).toBe(503)
    expect(mensajeAmigable(err)).toBe(MENSAJE_503)
    expect(mensajeAmigable(err)).toContain("no está disponible")
  })

  it("5xx genérico no expone detalles internos", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(respuesta(500, { message: "NullPointerException en X" })))
    const err = await falla(get("/x"))
    expect(mensajeAmigable(err)).not.toContain("NullPointer")
  })

  it("error de red (fetch rechaza) -> status 0 con mensaje de conexión", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")))
    const err = await falla(get("/x"))
    expect(err).toBeInstanceOf(ApiError)
    expect(err.status).toBe(0)
    expect(mensajeAmigable(err)).toBe(MENSAJE_RED)
  })

  it("respuesta de error sin JSON usa un mensaje por defecto", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("<html>Bad gateway</html>", { status: 502 })))
    const err = await falla(get("/x"))
    expect(err.status).toBe(502)
    expect(err.message).toBe("Error 502")
  })
})
