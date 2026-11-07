import { memo } from "react"
import { Link } from "react-router-dom"
import { MapPinIcon, StarIcon } from "@heroicons/react/24/solid"
import type { HabitacionResponse } from "../../api/types"
import { formatCurrencyARS } from "../../util/format"
import { imagenDeHotel } from "../../util/hotelImage"
import { etiquetaAmenity } from "../../util/amenities"

interface Props {
  habitacion: HabitacionResponse
  /** Query con las fechas/huéspedes buscados, para que el detalle calcule el total. */
  busqueda?: string
  noches?: number
  distanciaKm?: number
}

function HabitacionCardBase({ habitacion: h, busqueda = "", noches = 0, distanciaKm }: Props) {
  const total = h.precioNoche != null && noches > 0 ? h.precioNoche * noches : null
  return (
    <article className="rarn-hotel-card" aria-label={`Habitación ${h.numero} de ${h.hotel.nombre}`}>
      <div className="rarn-hotel-card__media">
        <img src={imagenDeHotel(h.hotel.id)} alt={h.hotel.nombre} loading="lazy" decoding="async" />
        <div className="rarn-hotel-card__rating-badge" aria-label={`${h.hotel.categoria} estrellas`}>
          <StarIcon className="icon" />
          <span className="rarn-typography-span">{h.hotel.categoria}</span>
        </div>
      </div>

      <div className="rarn-hotel-card__body">
        <h3 className="rarn-typography-h4 rarn-hotel-card__title">{h.hotel.nombre}</h3>
        <div className="rarn-hotel-card__location">
          <MapPinIcon className="icon" />
          <span className="rarn-typography-span">
            {h.hotel.domicilio}
            {distanciaKm != null && ` · a ${distanciaKm.toFixed(1)} km`}
          </span>
        </div>
        <p className="text-sm mb-2">
          Habitación {h.numero} · {h.tipoHabitacion} · hasta {h.capacidad} huésped{h.capacidad === 1 ? "" : "es"}
        </p>
        <div className="rarn-tags">
          {h.amenities.slice(0, 4).map((a) => (
            <span key={a} className="rarn-chip">
              {etiquetaAmenity(a)}
            </span>
          ))}
          {h.amenities.length > 4 && <span className="rarn-chip">+{h.amenities.length - 4}</span>}
        </div>

        <div className="rarn-hotel-card__footer">
          <div className="rarn-hotel-card__price">
            {h.precioNoche != null ? (
              <>
                <div className="rarn-hotel-card__price-stack">
                  <span className="rarn-typography-h4 price-value">{formatCurrencyARS(h.precioNoche)}</span>{" "}
                  <span className="rarn-typography-span muted">por noche</span>
                </div>
                {total != null && (
                  <span className="rarn-typography-span muted">
                    Total {noches} noche{noches === 1 ? "" : "s"}: {formatCurrencyARS(total)}
                  </span>
                )}
              </>
            ) : (
              <span className="rarn-typography-span muted">Sin tarifa vigente</span>
            )}
          </div>
          <Link to={`/habitaciones/${h.habitacionId}${busqueda}`} className="rarn-hotel-card__button">
            Ver detalle
          </Link>
        </div>
      </div>
    </article>
  )
}

export const HabitacionCard = memo(HabitacionCardBase)
