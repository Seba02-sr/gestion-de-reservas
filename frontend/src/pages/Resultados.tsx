import React, { useMemo, useState } from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import { buscarHabitaciones } from "../api/habitaciones"
import type { BusquedaHabitaciones, HabitacionResponse } from "../api/types"
import { HabitacionCard } from "../components/habitaciones/HabitacionCard"
import Alert from "../components/ui/Alert"
import Button from "../components/ui/Button"
import Card from "../components/ui/Card"
import { Field, Input, Select } from "../components/ui/Field"
import { useAsync } from "../hooks/useAsync"
import { AMENITIES } from "../util/amenities"
import { formatFecha, nochesEntre } from "../util/date"
import { distanciaKm } from "../util/geo"
import { ordenar, type Orden } from "../util/orden"

const num = (v: string | null): number | undefined => {
  if (v === null || v.trim() === "") return undefined
  const n = Number(v)
  return Number.isFinite(n) ? n : undefined
}

const Resultados: React.FC = () => {
  const [params] = useSearchParams()
  const navigate = useNavigate()

  // Búsqueda que viene de la URL (SearchBar / hoteles destacados)
  const base = useMemo<BusquedaHabitaciones>(
    () => ({
      checkIn: params.get("checkIn") ?? undefined,
      checkOut: params.get("checkOut") ?? undefined,
      cantidadHuespedes: num(params.get("cantidadHuespedes")),
      lat: num(params.get("lat")),
      lng: num(params.get("lng")),
      radioKm: num(params.get("radioKm")),
      hotelId: num(params.get("hotelId")),
    }),
    [params]
  )

  // Filtros del panel lateral: se editan en `form` y se envían al backend al tocar "Aplicar"
  const [form, setForm] = useState({ precioMin: "", precioMax: "", estrellas: "", amenities: [] as string[] })
  const [aplicados, setAplicados] = useState(form)
  const [orden, setOrden] = useState<Orden>("relevancia")

  const consulta: BusquedaHabitaciones = {
    ...base,
    precioMin: num(aplicados.precioMin),
    precioMax: num(aplicados.precioMax),
    estrellas: num(aplicados.estrellas),
    amenities: aplicados.amenities,
  }

  const { data, loading, error, reload } = useAsync(
    (signal) => buscarHabitaciones(consulta, signal),
    [JSON.stringify(consulta)]
  )

  const noches = nochesEntre(base.checkIn, base.checkOut)
  const hayPunto = base.lat !== undefined && base.lng !== undefined
  const dist = (h: HabitacionResponse) =>
    hayPunto ? distanciaKm(base.lat!, base.lng!, h.hotel.latitud, h.hotel.longitud) : undefined

  const lista = useMemo(
    () => ordenar(data ?? [], orden, dist),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [data, orden, base.lat, base.lng]
  )

  const busquedaQuery = useMemo(() => {
    const p = new URLSearchParams()
    if (base.checkIn && base.checkOut) {
      p.set("checkIn", base.checkIn)
      p.set("checkOut", base.checkOut)
    }
    if (base.cantidadHuespedes) p.set("cantidadHuespedes", String(base.cantidadHuespedes))
    const s = p.toString()
    return s ? `?${s}` : ""
  }, [base])

  const nombreHotel = base.hotelId !== undefined ? lista[0]?.hotel.nombre ?? `hotel #${base.hotelId}` : null

  const quitarHotel = () => {
    const p = new URLSearchParams(params)
    p.delete("hotelId")
    const resto = p.toString()
    navigate(resto ? `/buscar?${resto}` : "/buscar")
  }

  const toggleAmenity = (a: string) =>
    setForm((f) => ({ ...f, amenities: f.amenities.includes(a) ? f.amenities.filter((x) => x !== a) : [...f.amenities, a] }))

  return (
    <main className="rarn-page">
      <h1 className="rarn-page__title">{nombreHotel ? `Habitaciones de ${nombreHotel}` : "Habitaciones disponibles"}</h1>
      <p className="rarn-page__subtitle">
        {params.get("destino") && <>Cerca de {params.get("destino")} · </>}
        {base.checkIn && base.checkOut
          ? `${formatFecha(base.checkIn)} → ${formatFecha(base.checkOut)} (${noches} noche${noches === 1 ? "" : "s"})`
          : "Sin fechas: se muestran todas las habitaciones habilitadas"}
        {base.cantidadHuespedes ? ` · ${base.cantidadHuespedes}+ huéspedes` : ""}
      </p>
      {base.hotelId !== undefined && (
        <p className="mb-4">
          <span className="rarn-chip">
            Hotel filtrado
            <button type="button" className="rarn-chip__remove" onClick={quitarHotel} aria-label="Quitar filtro de hotel">
              ×
            </button>
          </span>
        </p>
      )}

      <div className="rarn-results">
        <Card className="rarn-filters" title="Filtros">
          <form
            className="rarn-form"
            onSubmit={(e) => {
              e.preventDefault()
              setAplicados(form)
            }}
          >
            <Field label="Precio mínimo (por noche)" htmlFor="pmin">
              <Input id="pmin" type="number" min={0} value={form.precioMin} onChange={(e) => setForm({ ...form, precioMin: e.target.value })} />
            </Field>
            <Field label="Precio máximo (por noche)" htmlFor="pmax">
              <Input id="pmax" type="number" min={0} value={form.precioMax} onChange={(e) => setForm({ ...form, precioMax: e.target.value })} />
            </Field>
            <Field label="Estrellas (mínimo)" htmlFor="estrellas">
              <Select id="estrellas" value={form.estrellas} onChange={(e) => setForm({ ...form, estrellas: e.target.value })}>
                <option value="">Todas</option>
                {[1, 2, 3, 4, 5].map((n) => (
                  <option key={n} value={n}>
                    {n}+ ★
                  </option>
                ))}
              </Select>
            </Field>
            <fieldset>
              <legend className="rarn-label mb-1">Amenities (todos los elegidos)</legend>
              <div className="rarn-filters__amenities">
                {Object.entries(AMENITIES).map(([valor, etiqueta]) => (
                  <label key={valor}>
                    <input type="checkbox" checked={form.amenities.includes(valor)} onChange={() => toggleAmenity(valor)} />
                    {etiqueta}
                  </label>
                ))}
              </div>
            </fieldset>
            <Button type="submit" className="rarn-btn--block">
              Aplicar filtros
            </Button>
          </form>
        </Card>

        <section aria-live="polite">
          <div className="rarn-results__bar">
            <span className="text-sm">{loading ? "Buscando..." : `${lista.length} ${lista.length === 1 ? "habitación" : "habitaciones"}`}</span>
            <div className="w-56">
              <Select aria-label="Ordenar por" value={orden} onChange={(e) => setOrden(e.target.value as Orden)}>
                <option value="relevancia">Orden: relevancia</option>
                <option value="precio-asc">Precio: menor a mayor</option>
                <option value="precio-desc">Precio: mayor a menor</option>
                <option value="estrellas">Más estrellas</option>
                {hayPunto && <option value="distancia">Más cerca</option>}
              </Select>
            </div>
          </div>

          {error && (
            <Alert kind="error">
              {error}{" "}
              <button type="button" className="rarn-link" onClick={reload}>
                Reintentar
              </button>
            </Alert>
          )}

          {!loading && !error && lista.length === 0 && (
            <div className="rarn-empty">
              <p>No encontramos habitaciones con esos criterios.</p>
              <Link to="/" className="rarn-link">
                Volver a buscar
              </Link>
            </div>
          )}

          <div className="rarn-results__grid">
            {lista.map((h) => (
              <HabitacionCard
                key={h.habitacionId}
                habitacion={h}
                busqueda={busquedaQuery}
                noches={noches}
                distanciaKm={hayPunto ? dist(h) : undefined}
              />
            ))}
          </div>
        </section>
      </div>
    </main>
  )
}

export default Resultados
