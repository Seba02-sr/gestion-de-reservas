import hotel1 from "@/assets/images/hotel1.webp"
import hotel2 from "@/assets/images/hotel2.webp"
import hotel3 from "@/assets/images/hotel3.webp"
import hotel4 from "@/assets/images/hotel4.webp"
import hotel5 from "@/assets/images/hotel5.webp"
import hotel6 from "@/assets/images/hotel6.webp"

const IMAGENES = [hotel1, hotel2, hotel3, hotel4, hotel5, hotel6]

/** Imagen determinística por id de hotel: el mismo hotel siempre muestra la misma foto. */
export function imagenDeHotel(id: number): string {
  const i = ((Math.trunc(id) % IMAGENES.length) + IMAGENES.length) % IMAGENES.length
  return IMAGENES[i]
}
