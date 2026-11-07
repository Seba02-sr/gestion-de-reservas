package edu.utn.frsf.isi.dan.reservas_svc.service;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.testutil.MongoQueries;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La regla de superposición con casos concretos (la misma tabla se prueba contra Mongo). */
public class DisponibilidadTest {

  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

  /**
   * Caso: reserva [in, out) (out null = abierta), estado, pedido [in, out) y resultado esperado.
   */
  public record Caso(
      String nombre,
      String resIn,
      String resOut,
      EstadoReserva estado,
      String pedIn,
      String pedOut,
      boolean ocupada) {}

  public static final List<Caso> CASOS =
      List.of(
          new Caso(
              "pedido idéntico",
              "2026-12-10",
              "2026-12-15",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              true),
          new Caso(
              "pedido dentro de la reserva",
              "2026-12-10",
              "2026-12-20",
              EstadoReserva.CONFIRMADA,
              "2026-12-12",
              "2026-12-14",
              true),
          new Caso(
              "pedido que contiene a la reserva",
              "2026-12-12",
              "2026-12-14",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-20",
              true),
          new Caso(
              "solapa el principio del pedido",
              "2026-12-08",
              "2026-12-11",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              true),
          new Caso(
              "solapa el final del pedido",
              "2026-12-14",
              "2026-12-18",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              true),
          new Caso(
              "la reserva termina el día que empieza el pedido (cambio de huésped)",
              "2026-12-05",
              "2026-12-10",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "la reserva empieza el día que termina el pedido",
              "2026-12-15",
              "2026-12-20",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "reserva totalmente anterior",
              "2026-11-01",
              "2026-11-05",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "reserva totalmente posterior",
              "2027-01-01",
              "2027-01-05",
              EstadoReserva.RESERVADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "reserva CANCELADA nunca ocupa",
              "2026-12-10",
              "2026-12-15",
              EstadoReserva.CANCELADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "CERRADA abierta, empieza antes del pedido",
              "2026-11-04",
              null,
              EstadoReserva.CERRADA,
              "2026-12-10",
              "2026-12-15",
              true),
          new Caso(
              "CERRADA abierta, empieza durante el pedido",
              "2026-12-12",
              null,
              EstadoReserva.CERRADA,
              "2026-12-10",
              "2026-12-15",
              true),
          new Caso(
              "CERRADA abierta, empieza el día de la salida del pedido",
              "2026-12-15",
              null,
              EstadoReserva.CERRADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "CERRADA abierta, empieza después del pedido",
              "2027-02-01",
              null,
              EstadoReserva.CERRADA,
              "2026-12-10",
              "2026-12-15",
              false),
          new Caso(
              "BLOQUEADA con fechas",
              "2026-12-11",
              "2026-12-13",
              EstadoReserva.BLOQUEADA,
              "2026-12-10",
              "2026-12-15",
              true),
          new Caso(
              "pedido de una sola noche dentro de una reserva larga",
              "2026-12-01",
              "2026-12-31",
              EstadoReserva.ADEUDADA,
              "2026-12-15",
              "2026-12-16",
              true));

  public static Instant dia(String fecha) {
    return fecha == null ? null : LocalDate.parse(fecha).atStartOfDay(ZONA).toInstant();
  }

  @Test
  void seSuperpone_tablaDeCasos() {
    for (Caso c : CASOS) {
      assertEquals(
          c.ocupada(),
          Disponibilidad.seSuperpone(
              dia(c.resIn()), dia(c.resOut()), c.estado(), dia(c.pedIn()), dia(c.pedOut())),
          c.nombre());
    }
  }

  @Test
  void laFormulaDelEnunciado_noDetectaUnaReservaQueEmpiezaEnMedioDelPedido() {
    // Slide 15 tal cual: ocupada si reserva.checkIn <= pedido.checkIn && reserva.checkOut >=
    // reserva.checkIn
    // (comparar la entrada con la entrada). Una reserva que empieza el 12 dentro de un pedido
    // 10→15 tiene checkIn(12) <= pedido.checkIn(10) == false, así que no se vería: por eso se usa
    // la regla de superposición de intervalos.
    Instant resIn = dia("2026-12-12");
    Instant resOut = dia("2026-12-14");
    Instant pedIn = dia("2026-12-10");
    boolean formulaDelPdf = !resIn.isAfter(pedIn) && !resOut.isBefore(resIn);

    assertFalse(formulaDelPdf);
    assertTrue(
        Disponibilidad.seSuperpone(
            resIn, resOut, EstadoReserva.RESERVADA, pedIn, dia("2026-12-15")));
  }

  @Test
  void libre_generaUnNorSobreElemMatchConLaMismaCondicion() {
    String json =
        MongoQueries.mapear(Disponibilidad.libre(dia("2026-12-10"), dia("2026-12-15"))).toString();

    assertTrue(json.contains("$nor"), json);
    assertTrue(json.contains("$elemMatch"), json);
    assertTrue(json.contains("CANCELADA"), json);
    assertTrue(json.contains("$or"), json);
  }
}
