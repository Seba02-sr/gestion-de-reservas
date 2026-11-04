package edu.utn.frsf.isi.dan.reservas_svc.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class TiempoConfigTest {

  private static final String BA = "America/Argentina/Buenos_Aires";

  @Test
  void sinFechaActual_usaElRelojDelSistemaEnLaZonaDeNegocio() {
    for (String vacio : new String[] {"", "  ", null}) {
      Clock clock = TiempoConfig.crearClock(BA, vacio);

      assertEquals(ZoneId.of(BA), clock.getZone());
      assertTrue(
          Math.abs(clock.instant().until(Instant.now(), java.time.temporal.ChronoUnit.SECONDS))
              < 5);
    }
  }

  @Test
  void conFechaActual_fijaElDiaYNoAvanza() {
    Clock clock = TiempoConfig.crearClock(BA, "2027-01-15");

    assertEquals(LocalDate.of(2027, 1, 15), LocalDate.now(clock));
    assertEquals(clock.instant(), clock.instant());
    assertEquals(ZoneId.of(BA), clock.getZone());
  }

  @Test
  void conFechaActual_elDiaEsElPedidoAunqueElServidorEsteEnUtc() {
    // 12:00 en Buenos Aires = 15:00 UTC, el mismo día en ambas zonas
    Clock clock = TiempoConfig.crearClock(BA, "2026-12-31");

    assertEquals(Instant.parse("2026-12-31T15:00:00Z"), clock.instant());
  }

  @Test
  void fechaActualInvalida_fallaConUnMensajeClaro() {
    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> TiempoConfig.crearClock(BA, "15/01/2027"));
    assertTrue(e.getMessage().contains("app.fecha-actual"));
    assertTrue(e.getMessage().contains("yyyy-MM-dd"));
  }

  @Test
  void zonaInvalida_fallaConUnMensajeClaro() {
    IllegalStateException e =
        assertThrows(
            IllegalStateException.class, () -> TiempoConfig.crearClock("Marte/Olimpo", ""));
    assertTrue(e.getMessage().contains("app.zone"));
  }
}
