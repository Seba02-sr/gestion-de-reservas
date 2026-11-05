package edu.utn.frsf.isi.dan.reservas_svc.service;

import static org.junit.jupiter.api.Assertions.*;

import edu.utn.frsf.isi.dan.reservas_svc.exception.ConflictoException;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Pago;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Review;
import edu.utn.frsf.isi.dan.reservas_svc.model.Tarifa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservaPolicyTest {

  private static Pago pago(double monto) {
    return Pago.builder()
        .method("TARJETA")
        .amount(new Tarifa(monto, "ARS"))
        .status("COMPLETED")
        .build();
  }

  private static Reserva reserva(EstadoReserva estado, double precioTotal, double... pagos) {
    List<Pago> lista = new ArrayList<>();
    for (double p : pagos) {
      lista.add(pago(p));
    }
    return Reserva.builder().estadoReserva(estado).precioTotal(precioTotal).pago(lista).build();
  }

  private static Review review() {
    return Review.builder().rating(4).comment("ok").build();
  }

  // ------------------------------------------------------------------ precio

  @Test
  void noches_cuentaLosDiasEntreCheckInYCheckOut() {
    assertEquals(5, ReservaPolicy.noches(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15)));
    assertEquals(1, ReservaPolicy.noches(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)));
    assertEquals(30, ReservaPolicy.noches(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 3)));
  }

  @Test
  void precioTotal_esNochesPorPrecioNoche() {
    assertEquals(75000.0, ReservaPolicy.precioTotal(15000.0, 5));
    assertEquals(15000.0, ReservaPolicy.precioTotal(15000.0, 1));
    assertEquals(0.0, ReservaPolicy.precioTotal(15000.0, 0));
  }

  @Test
  void precioTotal_redondeaADosDecimalesSinErroresDeComa() {
    assertEquals(0.3, ReservaPolicy.precioTotal(0.1, 3));
    assertEquals(33333.33, ReservaPolicy.precioTotal(11111.111, 3));
    assertEquals(100.01, ReservaPolicy.precioTotal(33.336, 3));
  }

  // -------------------------------------------------------------------- pagos

  @Test
  void totalPagadoYSaldo() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 20000, 30000);

    assertEquals(0, new BigDecimal("50000").compareTo(ReservaPolicy.totalPagado(r)));
    assertEquals(0, new BigDecimal("25000").compareTo(ReservaPolicy.saldo(r)));
    assertFalse(ReservaPolicy.pagoCompleto(r));
  }

  @Test
  void pagoCompleto_cuandoLoPagadoCubreElTotal() {
    assertTrue(ReservaPolicy.pagoCompleto(reserva(EstadoReserva.CONFIRMADA, 75000, 75000)));
    assertTrue(ReservaPolicy.pagoCompleto(reserva(EstadoReserva.CONFIRMADA, 75000, 50000, 25000)));
    assertFalse(ReservaPolicy.pagoCompleto(reserva(EstadoReserva.RESERVADA, 75000)));
  }

  @Test
  void confirmacion_conAlMenosUnPago_regla_slide14() {
    // un pago chico alcanza (slide 14); la mitad (slide 12) NO se exige
    assertTrue(ReservaPolicy.pagoAlcanzaParaConfirmar(reserva(EstadoReserva.RESERVADA, 75000, 1)));
    assertFalse(ReservaPolicy.pagoAlcanzaParaConfirmar(reserva(EstadoReserva.RESERVADA, 75000)));
  }

  @Test
  void validarPago_aceptaMontosHastaElSaldo() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 50000);

    assertDoesNotThrow(() -> ReservaPolicy.validarPago(r, 25000));
    assertDoesNotThrow(() -> ReservaPolicy.validarPago(r, 100));
  }

  @Test
  void validarPago_rechazaMontosQueExcedenElSaldo() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 50000);

    assertThrows(ConflictoException.class, () -> ReservaPolicy.validarPago(r, 25000.01));
  }

  @Test
  void validarPago_rechazaEstadosQueNoAdmitenPagos() {
    for (EstadoReserva e :
        List.of(
            EstadoReserva.CANCELADA,
            EstadoReserva.FINALIZADA,
            EstadoReserva.BLOQUEADA,
            EstadoReserva.CERRADA)) {
      assertThrows(
          ConflictoException.class,
          () -> ReservaPolicy.validarPago(reserva(e, 75000), 100),
          e.name());
    }
    for (EstadoReserva e :
        List.of(EstadoReserva.RESERVADA, EstadoReserva.CONFIRMADA, EstadoReserva.ADEUDADA)) {
      assertDoesNotThrow(() -> ReservaPolicy.validarPago(reserva(e, 75000), 100), e.name());
    }
  }

  // ---------------------------------------------------------- máquina de estados

  @Test
  void cancelar_sinPagos_desdeReservadaOBloqueada() {
    assertEquals(
        EstadoReserva.CANCELADA,
        ReservaPolicy.estadoTrasPedir(
            reserva(EstadoReserva.RESERVADA, 75000), EstadoReserva.CANCELADA));
    assertEquals(
        EstadoReserva.CANCELADA,
        ReservaPolicy.estadoTrasPedir(
            reserva(EstadoReserva.BLOQUEADA, 0), EstadoReserva.CANCELADA));
  }

  @Test
  void cancelar_conPagos_esConflicto() {
    ConflictoException e =
        assertThrows(
            ConflictoException.class,
            () ->
                ReservaPolicy.estadoTrasPedir(
                    reserva(EstadoReserva.CONFIRMADA, 75000, 100), EstadoReserva.CANCELADA));
    assertTrue(e.getMessage().contains("pagos"));
  }

  @Test
  void cancelar_desdeEstadosTerminalesOFinalizados_esConflicto() {
    for (EstadoReserva e :
        List.of(
            EstadoReserva.CANCELADA,
            EstadoReserva.FINALIZADA,
            EstadoReserva.ADEUDADA,
            EstadoReserva.CERRADA)) {
      assertThrows(
          ConflictoException.class,
          () -> ReservaPolicy.estadoTrasPedir(reserva(e, 75000), EstadoReserva.CANCELADA),
          e.name());
    }
  }

  @Test
  void finalizar_conReviewDelDuenoYPagoCompleto_quedaFinalizada() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 75000);
    r.setHostReview(review());

    assertEquals(
        EstadoReserva.FINALIZADA, ReservaPolicy.estadoTrasPedir(r, EstadoReserva.FINALIZADA));
  }

  @Test
  void finalizar_sinReviewDelDueno_quedaAdeudada() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 75000);

    assertEquals(
        EstadoReserva.ADEUDADA, ReservaPolicy.estadoTrasPedir(r, EstadoReserva.FINALIZADA));
  }

  @Test
  void finalizar_conPagoIncompleto_quedaAdeudada() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 74999.99);
    r.setHostReview(review());

    assertEquals(
        EstadoReserva.ADEUDADA, ReservaPolicy.estadoTrasPedir(r, EstadoReserva.FINALIZADA));
  }

  @Test
  void finalizar_desdeReservadaSinPagos_quedaAdeudada() {
    Reserva r = reserva(EstadoReserva.RESERVADA, 75000);
    r.setHostReview(review()); // aunque tenga review: no tiene pagos

    assertEquals(
        EstadoReserva.ADEUDADA, ReservaPolicy.estadoTrasPedir(r, EstadoReserva.FINALIZADA));
  }

  @Test
  void finalizar_desdeAdeudada_sePuedeReintentarYCompletar() {
    Reserva r = reserva(EstadoReserva.ADEUDADA, 75000, 75000);
    assertEquals(
        EstadoReserva.ADEUDADA, ReservaPolicy.estadoTrasPedir(r, EstadoReserva.FINALIZADA));

    r.setHostReview(review());
    assertEquals(
        EstadoReserva.FINALIZADA, ReservaPolicy.estadoTrasPedir(r, EstadoReserva.FINALIZADA));
  }

  @Test
  void finalizar_desdeEstadosInvalidos_esConflicto() {
    for (EstadoReserva e :
        List.of(
            EstadoReserva.CANCELADA,
            EstadoReserva.FINALIZADA,
            EstadoReserva.BLOQUEADA,
            EstadoReserva.CERRADA)) {
      assertThrows(
          ConflictoException.class,
          () -> ReservaPolicy.estadoTrasPedir(reserva(e, 75000, 75000), EstadoReserva.FINALIZADA),
          e.name());
    }
  }

  @Test
  void losDemasDestinos_noSePuedenPedirPorPatch() {
    Reserva r = reserva(EstadoReserva.RESERVADA, 75000);
    for (EstadoReserva destino :
        List.of(
            EstadoReserva.RESERVADA,
            EstadoReserva.CONFIRMADA,
            EstadoReserva.ADEUDADA,
            EstadoReserva.BLOQUEADA,
            EstadoReserva.CERRADA)) {
      assertThrows(
          ConflictoException.class,
          () -> ReservaPolicy.estadoTrasPedir(r, destino),
          destino.name());
    }
  }

  // ------------------------------------------------------------------- reviews

  @Test
  void reviewCliente_soloDespuesDelCheckOut() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 75000);
    LocalDate checkOut = LocalDate.of(2026, 12, 15);

    assertThrows(
        ConflictoException.class,
        () -> ReservaPolicy.validarReviewCliente(r, LocalDate.of(2026, 12, 14), checkOut));
    assertDoesNotThrow(
        () -> ReservaPolicy.validarReviewCliente(r, LocalDate.of(2026, 12, 15), checkOut));
    assertDoesNotThrow(
        () -> ReservaPolicy.validarReviewCliente(r, LocalDate.of(2027, 1, 20), checkOut));
  }

  @Test
  void reviewCliente_noSePuedeRepetirNiEnReservasSinHuesped() {
    Reserva ya = reserva(EstadoReserva.FINALIZADA, 75000, 75000);
    ya.setClientReview(review());
    LocalDate checkOut = LocalDate.of(2026, 12, 15);
    LocalDate hoy = LocalDate.of(2027, 1, 1);

    assertThrows(
        ConflictoException.class, () -> ReservaPolicy.validarReviewCliente(ya, hoy, checkOut));
    for (EstadoReserva e :
        List.of(EstadoReserva.CANCELADA, EstadoReserva.BLOQUEADA, EstadoReserva.CERRADA)) {
      assertThrows(
          ConflictoException.class,
          () -> ReservaPolicy.validarReviewCliente(reserva(e, 0), hoy, checkOut),
          e.name());
    }
  }

  @Test
  void reviewDueno_unaSolaVezYSoloEnReservasConHuesped() {
    assertDoesNotThrow(
        () -> ReservaPolicy.validarReviewDueno(reserva(EstadoReserva.RESERVADA, 75000)));
    Reserva ya = reserva(EstadoReserva.CONFIRMADA, 75000);
    ya.setHostReview(review());
    assertThrows(ConflictoException.class, () -> ReservaPolicy.validarReviewDueno(ya));
    assertThrows(
        ConflictoException.class,
        () -> ReservaPolicy.validarReviewDueno(reserva(EstadoReserva.CANCELADA, 0)));
  }
}
