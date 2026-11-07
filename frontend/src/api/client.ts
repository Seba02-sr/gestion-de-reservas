/**
 * Cliente HTTP central. Todos los pedidos van al API Gateway:
 *  - dev (Vite):   VITE_API_URL = http://localhost:8080 (el navegador llama directo al gateway, CORS en el gateway)
 *  - prod (nginx): VITE_API_URL = /api (mismo origen; nginx reenvía /api/** al gateway, sin CORS)
 * VITE_* se fija al COMPILAR (ver docs): no se puede cambiar con el contenedor ya construido.
 */

export const MENSAJE_503 = "El servicio no está disponible, probá en unos segundos."
export const MENSAJE_RED = "No se pudo conectar con el servidor. Revisá tu conexión y probá de nuevo."

/** Error devuelto por el backend: {message, path, timestamp, status}. */
export class ApiError extends Error {
  readonly status: number
  readonly path?: string

  constructor(status: number, message: string, path?: string) {
    super(message)
    this.name = "ApiError"
    this.status = status
    this.path = path
  }
}

export function apiUrl(): string {
  const base = (import.meta.env.VITE_API_URL as string | undefined) ?? "http://localhost:8080"
  return base.replace(/\/+$/, "")
}

type Query = Record<string, string | number | boolean | null | undefined | (string | number)[]>

function buildQuery(query?: Query): string {
  if (!query) return ""
  const params = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v === undefined || v === null || v === "") continue
    if (Array.isArray(v)) v.forEach((x) => params.append(k, String(x)))
    else params.append(k, String(v))
  }
  const s = params.toString()
  return s ? `?${s}` : ""
}

export interface RequestOptions {
  body?: unknown
  query?: Query
  signal?: AbortSignal
}

export async function request<T>(method: string, path: string, opts: RequestOptions = {}): Promise<T> {
  const init: RequestInit = { method, headers: { Accept: "application/json" }, signal: opts.signal }
  if (opts.body !== undefined) {
    init.headers = { ...init.headers, "Content-Type": "application/json" }
    init.body = JSON.stringify(opts.body)
  }

  let res: Response
  try {
    res = await fetch(`${apiUrl()}${path}${buildQuery(opts.query)}`, init)
  } catch (e) {
    if (e instanceof DOMException && e.name === "AbortError") throw e
    // fetch solo rechaza por red caída / CORS / DNS
    throw new ApiError(0, MENSAJE_RED)
  }

  const text = await res.text()
  let json: unknown = undefined
  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      json = undefined
    }
  }

  if (!res.ok) {
    const body = (json ?? {}) as { message?: string; path?: string }
    throw new ApiError(res.status, body.message || `Error ${res.status}`, body.path)
  }
  return json as T
}

export const get = <T>(path: string, query?: Query, signal?: AbortSignal) =>
  request<T>("GET", path, { query, signal })
export const post = <T>(path: string, body?: unknown) => request<T>("POST", path, { body })
export const put = <T>(path: string, body?: unknown) => request<T>("PUT", path, { body })
export const patch = <T>(path: string, body?: unknown) => request<T>("PATCH", path, { body })

/** Texto para mostrar al usuario según el tipo de error. */
export function mensajeAmigable(error: unknown): string {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 0:
        return error.message
      case 401:
        return error.message || "Usuario o contraseña incorrectos"
      case 503:
        return MENSAJE_503
      case 400:
      case 404:
      case 409:
        return error.message
      default:
        return error.status >= 500
          ? "Ocurrió un error inesperado en el servidor. Probá de nuevo en unos segundos."
          : error.message
    }
  }
  if (error instanceof Error) return error.message
  return "Ocurrió un error inesperado."
}
