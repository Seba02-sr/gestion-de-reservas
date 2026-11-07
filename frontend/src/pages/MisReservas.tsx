import React, { useState } from "react"
import { Link, useLocation } from "react-router-dom"
import { cambiarEstado, dejarReviewCliente, listarReservasDeHuesped, pagarReserva } from "../api/reservas"
import { mensajeAmigable } from "../api/client"
import type { ReservaResponse } from "../api/types"
import Alert from "../components/ui/Alert"
import Button from "../components/ui/Button"
import Card from "../components/ui/Card"
import { Field, Input, Select } from "../components/ui/Field"
import { useAuth } from "../context/auth"
import { useAsync } from "../hooks/useAsync"
import { formatFecha } from "../util/date"
import { formatCurrencyARS } from "../util/format"
import { puedeCancelar, puedeDejarReview, puedePagar } from "../util/reservaReglas"

type Modal = { tipo: "pago" | "review"; reserva: ReservaResponse } | null

const MisReservas: React.FC = () => {
  const { user } = useAuth()
  const location = useLocation()
  const { data, loading, error, reload } = useAsync(
    (signal) => listarReservasDeHuesped(user!.userId, signal),
    [user?.userId]
  )
  const [modal, setModal] = useState<Modal>(null)
  const [aviso, setAviso] = useState<{ kind: "error" | "success"; texto: string } | null>(
    (location.state as { reservaCreada?: boolean } | null)?.reservaCreada
      ? { kind: "success", texto: "¡Reserva creada! Está RESERVADA: se confirma con el primer pago." }
      : null
  )
  const [trabajando, setTrabajando] = useState<string | null>(null)

  // Ejecuta una acción del backend y muestra su mensaje tal cual si falla (409, 503, ...)
  const ejecutar = async (id: string, texto: string, accion: () => Promise<unknown>) => {
    setAviso(null)
    setTrabajando(id)
    try {
      await accion()
      setAviso({ kind: "success", texto })
      reload()
      return true
    } catch (e) {
      setAviso({ kind: "error", texto: mensajeAmigable(e) })
      return false
    } finally {
      setTrabajando(null)
    }
  }

  const cancelar = (r: ReservaResponse) =>
    ejecutar(r.id, "La reserva fue cancelada.", () => cambiarEstado(r.id, "CANCELADA"))

  const reservas = [...(data ?? [])].sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""))

  return (
    <main className="rarn-page">
      <h1 className="rarn-page__title">Mis reservas</h1>
      <p className="rarn-page__subtitle">Hola {user?.nombre ?? user?.email}, estas son tus reservas.</p>

      {aviso && <Alert kind={aviso.kind}>{aviso.texto}</Alert>}
      {error && (
        <Alert kind="error">
          {error}{" "}
          <button type="button" className="rarn-link" onClick={reload}>
            Reintentar
          </button>
        </Alert>
      )}
      {loading && <p className="rarn-empty">Cargando reservas...</p>}
      {!loading && !error && reservas.length === 0 && (
        <div className="rarn-empty">
          <p>Todavía no tenés reservas.</p>
          <Link to="/" className="rarn-link">Buscar habitaciones</Link>
        </div>
      )}

      <div className="rarn-reservas">
        {reservas.map((r) => {
          const pagar = puedePagar(r)
          const cancelarOk = puedeCancelar(r)
          const review = puedeDejarReview(r)
          const ocupado = trabajando === r.id
          return (
            <Card key={r.id}>
              <div className="rarn-reserva">
                <div className="rarn-reserva__head">
                  <div>
                    <h2 className="font-bold">Habitación {r.habitacionId} · hotel #{r.hotelId}</h2>
                    <p className="text-sm">
                      {formatFecha(r.checkIn)} → {formatFecha(r.checkOut)}
                    </p>
                  </div>
                  <span className={`rarn-badge rarn-badge--${r.estado}`}>{r.estado}</span>
                </div>

                <dl className="rarn-reserva__grid">
                  <div><dt>Total</dt><dd>{formatCurrencyARS(r.precioTotal)}</dd></div>
                  <div><dt>Pagado</dt><dd>{formatCurrencyARS(r.totalPagado)}</dd></div>
                  <div><dt>Saldo</dt><dd>{formatCurrencyARS(r.saldo)}</dd></div>
                  <div><dt>Tu opinión</dt><dd>{r.clientReview ? `${r.clientReview.rating} ★` : "—"}</dd></div>
                </dl>

                <div className="rarn-reserva__actions">
                  <Button size="sm" disabled={!pagar.habilitada || ocupado} title={pagar.motivo} onClick={() => setModal({ tipo: "pago", reserva: r })}>
                    Pagar
                  </Button>
                  <Button size="sm" variant="ghost" className="rarn-btn--outline" disabled={!cancelarOk.habilitada || ocupado} title={cancelarOk.motivo} onClick={() => cancelar(r)}>
                    Cancelar reserva
                  </Button>
                  <Button size="sm" variant="ghost" className="rarn-btn--outline" disabled={!review.habilitada || ocupado} title={review.motivo} onClick={() => setModal({ tipo: "review", reserva: r })}>
                    Dejar opinión
                  </Button>
                </div>
              </div>
            </Card>
          )
        })}
      </div>

      {modal?.tipo === "pago" && (
        <ModalPago
          reserva={modal.reserva}
          onClose={() => setModal(null)}
          onConfirm={async (monto) => {
            const ok = await ejecutar(modal.reserva.id, "Pago registrado.", () =>
              pagarReserva(modal.reserva.id, { method: "TARJETA", monto })
            )
            if (ok) setModal(null)
          }}
        />
      )}
      {modal?.tipo === "review" && (
        <ModalReview
          onClose={() => setModal(null)}
          onConfirm={async (rating, comentario) => {
            const ok = await ejecutar(modal.reserva.id, "¡Gracias por tu opinión!", () =>
              dejarReviewCliente(modal.reserva.id, { rating, comentario: comentario || undefined })
            )
            if (ok) setModal(null)
          }}
        />
      )}
    </main>
  )
}

function ModalPago({ reserva, onClose, onConfirm }: { reserva: ReservaResponse; onClose: () => void; onConfirm: (monto: number) => void }) {
  const [monto, setMonto] = useState(String(reserva.saldo))
  const valor = Number(monto)
  const invalido = !(valor > 0) || valor > reserva.saldo
  return (
    <div className="rarn-modal-backdrop" role="dialog" aria-modal="true" aria-label="Registrar pago">
      <form className="rarn-modal" onSubmit={(e) => { e.preventDefault(); if (!invalido) onConfirm(valor) }}>
        <h2 className="rarn-modal__title">Pagar reserva</h2>
        <p className="text-sm">Saldo pendiente: {formatCurrencyARS(reserva.saldo)}. Podés pagar una parte o todo.</p>
        <Field label="Monto a pagar (ARS)" htmlFor="monto">
          <Input id="monto" type="number" min={1} max={reserva.saldo} step="0.01" value={monto} onChange={(e) => setMonto(e.target.value)} autoFocus />
          {invalido && <p className="rarn-form-error">El monto debe ser mayor a 0 y no superar el saldo.</p>}
        </Field>
        <div className="rarn-modal__actions">
          <Button type="button" variant="ghost" onClick={onClose}>Cerrar</Button>
          <Button type="submit" disabled={invalido}>Confirmar pago</Button>
        </div>
      </form>
    </div>
  )
}

function ModalReview({ onClose, onConfirm }: { onClose: () => void; onConfirm: (rating: number, comentario: string) => void }) {
  const [rating, setRating] = useState("5")
  const [comentario, setComentario] = useState("")
  return (
    <div className="rarn-modal-backdrop" role="dialog" aria-modal="true" aria-label="Dejar opinión">
      <form className="rarn-modal" onSubmit={(e) => { e.preventDefault(); onConfirm(Number(rating), comentario.trim()) }}>
        <h2 className="rarn-modal__title">Tu opinión</h2>
        <Field label="Calificación" htmlFor="rating">
          <Select id="rating" value={rating} onChange={(e) => setRating(e.target.value)}>
            {[5, 4, 3, 2, 1].map((n) => (
              <option key={n} value={n}>{n} ★</option>
            ))}
          </Select>
        </Field>
        <Field label="Comentario (opcional)" htmlFor="comentario">
          <Input id="comentario" value={comentario} maxLength={1000} onChange={(e) => setComentario(e.target.value)} />
        </Field>
        <div className="rarn-modal__actions">
          <Button type="button" variant="ghost" onClick={onClose}>Cerrar</Button>
          <Button type="submit">Enviar opinión</Button>
        </div>
      </form>
    </div>
  )
}

export default MisReservas
