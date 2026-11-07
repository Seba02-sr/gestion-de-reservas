import React, { useMemo, useState } from "react"
import { Link, useLocation, useNavigate, useParams, useSearchParams } from "react-router-dom"
import { StarIcon } from "@heroicons/react/24/solid"
import { obtenerHabitacion } from "../api/habitaciones"
import { crearReserva } from "../api/reservas"
import { mensajeAmigable } from "../api/client"
import Alert from "../components/ui/Alert"
import Button from "../components/ui/Button"
import Card from "../components/ui/Card"
import { Field, Input } from "../components/ui/Field"
import { useAuth } from "../context/auth"
import { useAsync } from "../hooks/useAsync"
import { etiquetaAmenity } from "../util/amenities"
import { isInvalidCheckOut, isPastDate, nochesEntre, todayISO } from "../util/date"
import { formatCurrencyARS } from "../util/format"
import { imagenDeHotel } from "../util/hotelImage"

const DetalleHabitacion: React.FC = () => {
  const { id } = useParams()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()

  const { data: h, loading, error } = useAsync((signal) => obtenerHabitacion(id!, signal), [id])

  const [checkIn, setCheckIn] = useState(params.get("checkIn") ?? "")
  const [checkOut, setCheckOut] = useState(params.get("checkOut") ?? "")
  const [reservando, setReservando] = useState(false)
  const [errorReserva, setErrorReserva] = useState<string | null>(null)
  const today = useMemo(() => todayISO(), [])

  const noches = nochesEntre(checkIn, checkOut)
  const errorFechas = isPastDate(checkIn)
    ? "El check-in no puede ser anterior a hoy."
    : checkIn && checkOut && (isInvalidCheckOut(checkIn, checkOut) || noches === 0)
      ? "El check-out debe ser posterior al check-in."
      : null
  const total = h?.precioNoche != null && noches > 0 ? h.precioNoche * noches : null
  const huespedes = Number(params.get("cantidadHuespedes")) || 0
  const excedeCapacidad = !!h && huespedes > h.capacidad

  const reservar = async () => {
    if (!h) return
    if (!user) {
      navigate(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`)
      return
    }
    setErrorReserva(null)
    setReservando(true)
    try {
      await crearReserva({ habitacionId: h.habitacionId, idHuesped: user.userId, checkIn, checkOut })
      navigate("/mis-reservas", { state: { reservaCreada: true } })
    } catch (e) {
      // 409: fechas ocupadas / sin tarifa / no disponible; 404: huésped inexistente; 503: servicio caído
      setErrorReserva(mensajeAmigable(e))
    } finally {
      setReservando(false)
    }
  }

  if (loading) return <main className="rarn-page"><p className="rarn-empty">Cargando habitación...</p></main>
  if (error || !h)
    return (
      <main className="rarn-page">
        <Alert kind="error">{error ?? "No se encontró la habitación."}</Alert>
        <Link to="/buscar" className="rarn-link">Volver a la búsqueda</Link>
      </main>
    )

  return (
    <main className="rarn-page">
      <p className="mb-4 text-sm">
        <button type="button" onClick={() => navigate(-1)} className="rarn-link">
          ← Volver
        </button>
      </p>
      <div className="rarn-detail">
        <section>
          <div className="rarn-detail__media">
            <img src={imagenDeHotel(h.hotel.id)} alt={h.hotel.nombre} />
          </div>
          <h1 className="rarn-page__title mt-4">{h.hotel.nombre}</h1>
          <p className="rarn-stars" aria-label={`${h.hotel.categoria} estrellas`}>
            {Array.from({ length: h.hotel.categoria }).map((_, i) => (
              <StarIcon key={i} className="inline h-5 w-5" />
            ))}
          </p>
          <p className="rarn-page__subtitle">{h.hotel.domicilio}</p>

          <div className="rarn-detail__meta">
            <span>Habitación {h.numero} · piso {h.piso}</span>
            <span>Tipo: {h.tipoHabitacion}</span>
            <span>Capacidad: {h.capacidad} huésped{h.capacidad === 1 ? "" : "es"}</span>
          </div>

          <h2 className="rarn-form__section mb-2">Amenities</h2>
          {h.amenities.length ? (
            <div className="rarn-tags">
              {h.amenities.map((a) => (
                <span key={a} className="rarn-chip">{etiquetaAmenity(a)}</span>
              ))}
            </div>
          ) : (
            <p className="text-sm">Sin amenities cargados.</p>
          )}
        </section>

        <aside>
          <Card title="Tu reserva">
            <div className="rarn-form">
              <p className="text-2xl font-extrabold">
                {h.precioNoche != null ? formatCurrencyARS(h.precioNoche) : "Sin tarifa"}
                {h.precioNoche != null && <span className="text-sm font-normal"> por noche</span>}
              </p>

              <Field label="Check-in" htmlFor="d-checkin">
                <Input id="d-checkin" type="date" min={today} value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
              </Field>
              <Field label="Check-out" htmlFor="d-checkout">
                <Input id="d-checkout" type="date" min={checkIn || today} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
              </Field>
              {errorFechas && <p className="rarn-form-error">{errorFechas}</p>}
              {excedeCapacidad && (
                <Alert kind="info">Buscaste para {huespedes} huéspedes y esta habitación admite hasta {h.capacidad}.</Alert>
              )}

              {total != null && (
                <div>
                  <div className="rarn-detail__total">
                    <span>{formatCurrencyARS(h.precioNoche!)} × {noches} noche{noches === 1 ? "" : "s"}</span>
                    <span>{formatCurrencyARS(total)}</span>
                  </div>
                  <div className="rarn-detail__total rarn-detail__total--strong">
                    <span>Total estimado</span>
                    <span>{formatCurrencyARS(total)}</span>
                  </div>
                </div>
              )}

              {errorReserva && <Alert kind="error">{errorReserva}</Alert>}

              <Button
                className="rarn-btn--block"
                onClick={reservar}
                disabled={reservando || !checkIn || !checkOut || !!errorFechas || h.precioNoche == null}
                title={h.precioNoche == null ? "Esta habitación todavía no tiene tarifa vigente" : undefined}
              >
                {reservando ? "Reservando..." : user ? "Reservar" : "Iniciar sesión para reservar"}
              </Button>
              <p className="rarn-form__hint">El total es una estimación: el precio final lo calcula el sistema al reservar.</p>
            </div>
          </Card>
        </aside>
      </div>
    </main>
  )
}

export default DetalleHabitacion
