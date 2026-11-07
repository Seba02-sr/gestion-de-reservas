import hotel1 from "@/assets/images/hotel1.webp"
import hotel2 from "@/assets/images/hotel2.webp"
import hotel3 from "@/assets/images/hotel3.webp"

const IMAGENES = [hotel1, hotel2, hotel3]

/** Imagen determinística por id de hotel: el mismo hotel siempre muestra la misma foto. */
export function imagenDeHotel(id: number): string {
  const i = ((Math.trunc(id) % IMAGENES.length) + IMAGENES.length) % IMAGENES.length
  return IMAGENES[i]
}
