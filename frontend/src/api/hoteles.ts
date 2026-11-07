import { get } from "./client"
import type { HotelResponse } from "./types"

/** Hoteles de gestion-svc (los cerrados vienen con cerrado=true). */
export const listarHoteles = (signal?: AbortSignal) => get<HotelResponse[]>("/gestion/hoteles", undefined, signal)

export async function listarHotelesAbiertos(signal?: AbortSignal): Promise<HotelResponse[]> {
  const todos = await listarHoteles(signal)
  return todos.filter((h) => !h.cerrado)
}
