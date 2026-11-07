package edu.utn.frsf.isi.dan.reservas_svc.config;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de negocio. El "hoy" de las reservas (validación del check-in, reviews después del
 * check-out) sale siempre de este {@link Clock} y en la zona {@code app.zone}.
 *
 * <p>Para probar a mano se puede fijar la fecha con {@code app.fecha-actual} (variable de entorno
 * {@code APP_FECHA_ACTUAL}), p. ej. {@code 2027-01-15}: el reloj queda fijo ese día a las 12:00.
 * Por defecto está vacía y se usa la fecha real.
 */
@Configuration
@Log4j2
public class TiempoConfig {

  @Bean
  public Clock clock(
      @Value("${app.zone:America/Argentina/Buenos_Aires}") String zona,
      @Value("${app.fecha-actual:}") String fechaActual) {
    return crearClock(zona, fechaActual);
  }

  /** Separado del bean para poder probarlo sin levantar Spring. */
  public static Clock crearClock(String zona, String fechaActual) {
    ZoneId zone;
    try {
      zone = ZoneId.of(zona);
    } catch (DateTimeException e) {
      throw new IllegalStateException("app.zone inválida: '" + zona + "'", e);
    }
    if (fechaActual == null || fechaActual.isBlank()) {
      return Clock.system(zone);
    }
    LocalDate fecha;
    try {
      fecha = LocalDate.parse(fechaActual.trim());
    } catch (DateTimeParseException e) {
      throw new IllegalStateException(
          "app.fecha-actual inválida: '" + fechaActual + "' (formato esperado: yyyy-MM-dd)", e);
    }
    log.warn(
        "*** FECHA SIMULADA: el servicio cree que hoy es {} (app.fecha-actual). Solo para pruebas. ***",
        fecha);
    return Clock.fixed(fecha.atTime(12, 0).atZone(zone).toInstant(), zone);
  }
}
