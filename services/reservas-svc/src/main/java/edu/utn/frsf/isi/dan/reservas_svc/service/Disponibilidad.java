package edu.utn.frsf.isi.dan.reservas_svc.service;

import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import java.time.Instant;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Regla de disponibilidad de una habitación, en dos versiones que deben coincidir: una en Java
 * ({@link #seSuperpone}) y otra como criterio de Mongo ({@link #libre}).
 *
 * <p>Una habitación está OCUPADA para el pedido {@code [pedidoIn, pedidoOut)} si tiene alguna
 * reserva que NO esté CANCELADA y cumpla:
 *
 * <pre>
 *   reserva.checkIn &lt; pedidoOut   Y   (reserva.checkOut es null   O   reserva.checkOut &gt; pedidoIn)
 * </pre>
 *
 * <p>Los intervalos son semiabiertos: el día de salida de una estadía no cuenta como ocupado, así
 * que otra puede empezar ese mismo día. El {@code null} cubre las reservas abiertas (CERRADA, o
 * BLOQUEADA sin fin): bloquean para siempre.
 *
 * <p>Nota sobre el enunciado: la slide 15 dice "no tiene reservas cuya fecha de ingreso sea menor o
 * igual que la fecha de entrada y la fecha de salida sea mayor o igual que la fecha de ingreso".
 * Redactado así compara la entrada con la entrada y no cubre todos los solapamientos (p. ej. una
 * reserva que empieza en medio del pedido). La fórmula correcta para detectar un solapamiento entre
 * dos intervalos es la de arriba (cada uno empieza antes de que el otro termine); además la slide
 * usa extremos cerrados (&lt;=, &gt;=), con lo cual un cambio de huésped el mismo día contaría como
 * conflicto.
 */
public final class Disponibilidad {

  private Disponibilidad() {}

  /** Versión Java de la regla. {@code estado} puede ser null (se trata como no cancelada). */
  public static boolean seSuperpone(
      Instant reservaIn,
      Instant reservaOut,
      EstadoReserva estado,
      Instant pedidoIn,
      Instant pedidoOut) {
    if (estado == EstadoReserva.CANCELADA) {
      return false;
    }
    return reservaIn.isBefore(pedidoOut) && (reservaOut == null || reservaOut.isAfter(pedidoIn));
  }

  /** Criterio (para usar dentro de {@code reservas}) de las reservas que ocupan el pedido. */
  public static Criteria reservaQueOcupa(Instant pedidoIn, Instant pedidoOut) {
    return Criteria.where("estadoReserva")
        .ne(EstadoReserva.CANCELADA.name())
        .and("checkIn")
        .lt(pedidoOut)
        .orOperator(Criteria.where("checkOut").is(null), Criteria.where("checkOut").gt(pedidoIn));
  }

  /** Criterio sobre la habitación: ninguna de sus reservas ocupa el pedido. */
  public static Criteria libre(Instant pedidoIn, Instant pedidoOut) {
    return new Criteria()
        .norOperator(Criteria.where("reservas").elemMatch(reservaQueOcupa(pedidoIn, pedidoOut)));
  }
}
