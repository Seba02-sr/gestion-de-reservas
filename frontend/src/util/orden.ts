import type { HabitacionResponse } from "../api/types"

export type Orden = "relevancia" | "precio-asc" | "precio-desc" | "estrellas" | "distancia"

/** Ordena en el cliente (el backend no ordena). Las habitaciones sin tarifa van al final en "precio menor". */
export function ordenar(
  lista: HabitacionResponse[],
  orden: Orden,
  dist: (h: HabitacionResponse) => number | undefined
): HabitacionResponse[] {
  const copia = [...lista]
  const precio = (h: HabitacionResponse) => h.precioNoche ?? Number.POSITIVE_INFINITY
  switch (orden) {
    case "precio-asc":
      return copia.sort((a, b) => precio(a) - precio(b))
    case "precio-desc":
      return copia.sort((a, b) => (b.precioNoche ?? -1) - (a.precioNoche ?? -1))
    case "estrellas":
      return copia.sort((a, b) => b.hotel.categoria - a.hotel.categoria)
    case "distancia":
      return copia.sort((a, b) => (dist(a) ?? Infinity) - (dist(b) ?? Infinity))
    default:
      return copia
  }
}
