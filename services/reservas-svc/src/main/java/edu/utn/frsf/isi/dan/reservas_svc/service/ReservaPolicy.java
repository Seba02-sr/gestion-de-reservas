package edu.utn.frsf.isi.dan.reservas_svc.service;

import edu.utn.frsf.isi.dan.reservas_svc.exception.ConflictoException;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Pago;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

/**
 * Reglas de negocio puras de una reserva (sin Mongo ni Spring): precio, pagos y máquina de estados.
 * Todo lo que cambia de estado pasa por acá.
 *
 * <pre>
 *            pago (≥ 1)                 PATCH FINALIZADA (review del dueño + pago completo)
 *  RESERVADA ───────────► CONFIRMADA ───────────────────────────────► FINALIZADA
 *     │  │                    │   └── falta review o pago ──► ADEUDADA ──┘ (reintentar PATCH FINALIZADA)
 *     │  └─ PATCH FINALIZADA (sin pagos) ───────────────────────────────► ADEUDADA
 *     └─ PATCH CANCELADA (solo sin pagos) ► CANCELADA        BLOQUEADA ─ PATCH CANCELADA ─► CANCELADA
 * </pre>
 */
public final class ReservaPolicy {

  private static final Set<EstadoReserva> NO_ADMITEN_PAGO =
      EnumSet.of(
          EstadoReserva.CANCELADA,
          EstadoReserva.FINALIZADA,
          EstadoReserva.BLOQUEADA,
          EstadoReserva.CERRADA);

  private static final Set<EstadoReserva> PUEDEN_FINALIZARSE =
      EnumSet.of(EstadoReserva.RESERVADA, EstadoReserva.CONFIRMADA, EstadoReserva.ADEUDADA);

  private static final Set<EstadoReserva> CON_HUESPED_ACTIVA =
      EnumSet.of(
          EstadoReserva.RESERVADA,
          EstadoReserva.CONFIRMADA,
          EstadoReserva.ADEUDADA,
          EstadoReserva.FINALIZADA);

  private ReservaPolicy() {}

  // ------------------------------------------------------------------- precio

  public static long noches(LocalDate checkIn, LocalDate checkOut) {
    return ChronoUnit.DAYS.between(checkIn, checkOut);
  }

  /** precioTotal = noches × precioNoche, redondeado a 2 decimales. */
  public static double precioTotal(double precioNoche, long noches) {
    return BigDecimal.valueOf(precioNoche)
        .multiply(BigDecimal.valueOf(noches))
        .setScale(2, RoundingMode.HALF_UP)
        .doubleValue();
  }

  // -------------------------------------------------------------------- pagos

  public static boolean tienePagos(Reserva reserva) {
    return reserva.getPago() != null && !reserva.getPago().isEmpty();
  }

  public static BigDecimal totalPagado(Reserva reserva) {
    if (!tienePagos(reserva)) {
      return BigDecimal.ZERO;
    }
    BigDecimal total = BigDecimal.ZERO;
    for (Pago pago : reserva.getPago()) {
      if (pago.getAmount() != null && pago.getAmount().getPrecio() != null) {
        total = total.add(BigDecimal.valueOf(pago.getAmount().getPrecio()));
      }
    }
    return total;
  }

  /** Lo que falta pagar (nunca negativo). */
  public static BigDecimal saldo(Reserva reserva) {
    if (reserva.getPrecioTotal() == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal saldo = BigDecimal.valueOf(reserva.getPrecioTotal()).subtract(totalPagado(reserva));
    return saldo.max(BigDecimal.ZERO);
  }

  public static boolean pagoCompleto(Reserva reserva) {
    return reserva.getPrecioTotal() != null
        && totalPagado(reserva).compareTo(BigDecimal.valueOf(reserva.getPrecioTotal())) >= 0;
  }

  /**
   * REGLA DE CONFIRMACIÓN (decisión de diseño): una reserva pasa a CONFIRMADA cuando tiene AL MENOS
   * UN PAGO (slide 14 del enunciado). La slide 12 dice, en cambio, "al menos la mitad": si se
   * quisiera esa variante, este es el único método a cambiar, p. ej. {@code totalPagado ≥
   * precioTotal / 2}. Está aislado a propósito.
   */
  public static boolean pagoAlcanzaParaConfirmar(Reserva reserva) {
    return tienePagos(reserva);
  }

  /** Valida que se pueda registrar un pago de {@code monto} (409 si no). */
  public static void validarPago(Reserva reserva, double monto) {
    EstadoReserva estado = reserva.getEstadoReserva();
    if (NO_ADMITEN_PAGO.contains(estado)) {
      throw new ConflictoException("No se pueden registrar pagos en una reserva " + estado);
    }
    if (BigDecimal.valueOf(monto).compareTo(saldo(reserva)) > 0) {
      throw new ConflictoException(
          "El pago (" + monto + ") excede el saldo pendiente (" + saldo(reserva) + ")");
    }
  }

  // ---------------------------------------------------------- máquina de estados

  /**
   * Estado resultante de pedir {@code destino} por PATCH /estado, o {@link ConflictoException} si
   * la transición no es válida.
   *
   * <ul>
   *   <li>CANCELADA: solo desde RESERVADA o BLOQUEADA y sin pagos.
   *   <li>FINALIZADA: desde RESERVADA, CONFIRMADA o ADEUDADA. Si hay review del dueño y el pago
   *       está completo queda FINALIZADA; si falta algo, queda ADEUDADA (desde RESERVADA no hay
   *       pagos, así que siempre queda ADEUDADA).
   *   <li>Cualquier otro destino no se puede pedir a mano (CONFIRMADA sale de un pago, ADEUDADA de
   *       finalizar, CERRADA de un evento, BLOQUEADA de POST /reservas/bloqueos, RESERVADA al
   *       crear).
   * </ul>
   */
  public static EstadoReserva estadoTrasPedir(Reserva reserva, EstadoReserva destino) {
    EstadoReserva actual = reserva.getEstadoReserva();
    switch (destino) {
      case CANCELADA:
        if (tienePagos(reserva)) {
          throw new ConflictoException("No se puede cancelar una reserva que tiene pagos");
        }
        if (actual != EstadoReserva.RESERVADA && actual != EstadoReserva.BLOQUEADA) {
          throw new ConflictoException("No se puede cancelar una reserva " + actual);
        }
        return EstadoReserva.CANCELADA;
      case FINALIZADA:
        if (!PUEDEN_FINALIZARSE.contains(actual)) {
          throw new ConflictoException("No se puede finalizar una reserva " + actual);
        }
        return reserva.getHostReview() != null && pagoCompleto(reserva)
            ? EstadoReserva.FINALIZADA
            : EstadoReserva.ADEUDADA;
      default:
        throw new ConflictoException(
            "El estado "
                + destino
                + " no se puede establecer manualmente (reserva "
                + actual
                + ")");
    }
  }

  // ------------------------------------------------------------------- reviews

  /** El huésped califica solo después del check-out (el día del check-out ya vale). */
  public static void validarReviewCliente(Reserva reserva, LocalDate hoy, LocalDate checkOut) {
    if (!CON_HUESPED_ACTIVA.contains(reserva.getEstadoReserva())) {
      throw new ConflictoException(
          "No se puede calificar una reserva " + reserva.getEstadoReserva());
    }
    if (checkOut == null || hoy.isBefore(checkOut)) {
      throw new ConflictoException(
          "Solo se puede calificar después del check-out (" + checkOut + ")");
    }
    if (reserva.getClientReview() != null) {
      throw new ConflictoException("La reserva ya tiene la calificación del huésped");
    }
  }

  public static void validarReviewDueno(Reserva reserva) {
    if (!CON_HUESPED_ACTIVA.contains(reserva.getEstadoReserva())) {
      throw new ConflictoException(
          "No se puede dejar el review del dueño en una reserva " + reserva.getEstadoReserva());
    }
    if (reserva.getHostReview() != null) {
      throw new ConflictoException("La reserva ya tiene el review del dueño");
    }
  }
}
