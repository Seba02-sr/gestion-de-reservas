// src/components/hoteles/HotelesDestacados.tsx
import { useNavigate } from "react-router-dom"
import { HotelCard } from "./HotelCard"
import type { Hotel } from "../../util/hotel"
import SectionHeader from "../ui/SectionHeader"
import Section from "../ui/Section"
import Alert from "../ui/Alert"
import { listarHotelesAbiertos } from "../../api/hoteles"
import { buscarHabitaciones } from "../../api/habitaciones"
import { useAsync } from "../../hooks/useAsync"
import { imagenDeHotel } from "../../util/hotelImage"

const CANTIDAD = 3

async function cargarDestacados(signal: AbortSignal): Promise<Hotel[]> {
  const hoteles = (await listarHotelesAbiertos(signal)).slice(0, CANTIDAD)

  // Precio "desde": mínimo precioNoche por hotel entre las habitaciones replicadas en reservas-svc.
  // Si esa consulta falla, igual se muestran los hoteles (sin precio).
  const minimos = new Map<number, number>()
  try {
    for (const h of await buscarHabitaciones({}, signal)) {
      if (h.precioNoche == null) continue
      const actual = minimos.get(h.hotel.id)
      if (actual === undefined || h.precioNoche < actual) minimos.set(h.hotel.id, h.precioNoche)
    }
  } catch {
    /* sin precios */
  }

  return hoteles.map((h) => ({
    id: h.id,
    nombre: h.nombre,
    ubicacion: h.domicilio,
    rating: h.categoria,
    precio: minimos.get(h.id),
    imagen: imagenDeHotel(h.id),
  }))
}

export default function HotelesDestacados() {
  const navigate = useNavigate()
  const { data, loading, error, reload } = useAsync(cargarDestacados, [])

  return (
    <Section className="hoteles" containerClassName="hoteles__container" id="hoteles">
      <SectionHeader
        className="hoteles__header"
        titleClassName="hoteles__title rarn-typography-h2"
        subtitleClassName="hoteles__subtitle rarn-typography-p"
        title="Hoteles Destacados"
        subtitle="Descubre los mejores alojamientos seleccionados para ti"
      />

      {loading && <p className="rarn-empty">Cargando hoteles...</p>}

      {error && (
        <Alert kind="error">
          {error}{" "}
          <button type="button" className="rarn-link" onClick={reload}>
            Reintentar
          </button>
        </Alert>
      )}

      {!loading && !error && data?.length === 0 && (
        <p className="rarn-empty">Todavía no hay hoteles cargados.</p>
      )}

      <div className="hoteles__grid">
        {data?.map((hotel) => (
          <HotelCard key={hotel.id} hotel={hotel} onVerDetalles={(id) => navigate(`/buscar?hotelId=${id}`)} />
        ))}
      </div>

      <div className="hoteles__actions">
        <button onClick={() => navigate("/buscar")} className="rarn-btn rarn-btn--primary rarn-btn--lg">
          Ver todas las habitaciones
        </button>
      </div>
    </Section>
  )
}
