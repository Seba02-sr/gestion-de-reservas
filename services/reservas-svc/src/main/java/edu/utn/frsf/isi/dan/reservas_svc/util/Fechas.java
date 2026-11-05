package edu.utn.frsf.isi.dan.reservas_svc.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Las reservas trabajan con días (check-in / check-out) pero se guardan como {@link Instant}: el
 * día {@code d} se representa con la medianoche de {@code d} en la zona de negocio (por defecto
 * America/Argentina/Buenos_Aires), así todas las comparaciones son sobre el mismo eje de tiempo.
 */
public final class Fechas {

  private Fechas() {}

  public static Instant inicioDelDia(LocalDate dia, ZoneId zona) {
    return dia == null ? null : dia.atStartOfDay(zona).toInstant();
  }

  public static LocalDate aFecha(Instant instante, ZoneId zona) {
    return instante == null ? null : instante.atZone(zona).toLocalDate();
  }
}
