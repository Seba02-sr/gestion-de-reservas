// src/components/hoteles/HotelCard.tsx
"use client"

import { memo } from "react"
import { MapPinIcon, StarIcon } from "@heroicons/react/24/solid"
import type { Hotel } from "../../util/hotel"
import { formatCurrencyARS } from "../../util/format"

interface HotelCardProps {
  hotel: Hotel
  onVerDetalles?: (id: number) => void
}

function HotelCardBase({ hotel, onVerDetalles }: HotelCardProps) {
  return (
    <article className="rarn-hotel-card" role="region" aria-label={`Hotel destacado: ${hotel.nombre}`}>
      {/* Media */}
      <div className="rarn-hotel-card__media">
        <img src={hotel.imagen || "/placeholder.svg"} alt={hotel.nombre} loading="lazy" decoding="async" />
        <div className="rarn-hotel-card__overlay">
          <span className="rarn-typography-h5">{hotel.nombre}</span>
        </div>
        {/* Rating en esquina */}
        <div className="rarn-hotel-card__rating-badge" aria-label={`Calificación ${hotel.rating}`}>
          <StarIcon className="icon" />
          <span className="rarn-typography-span">{hotel.rating}</span>
        </div>
      </div>

      {/* Body */}
      <div className="rarn-hotel-card__body">
        {/* Título */}
        <h3 className="rarn-typography-h4 rarn-hotel-card__title">{hotel.nombre}</h3>

        {/* Ubicación */}
        <div className="rarn-hotel-card__location">
          <MapPinIcon className="icon" />
          <span className="rarn-typography-span">{hotel.ubicacion}</span>
        </div>

        {/* Footer: precio + botón */}
        <div className="rarn-hotel-card__footer">
          <div className="rarn-hotel-card__price">
            <span className="rarn-typography-span muted">Desde</span>
            <div className="rarn-hotel-card__price-stack">
              <span className="rarn-typography-h4 price-value">{formatCurrencyARS(hotel.precio)}</span>
              <span className="rarn-typography-span muted">por noche</span>
            </div>
          </div>

          <button
            type="button"
            className="rarn-hotel-card__button"
            onClick={() => onVerDetalles?.(hotel.id)}
          >
            Ver detalles
          </button>
        </div>
      </div>
    </article>
  )
}

export const HotelCard = memo(HotelCardBase)
