package edu.utn.frsf.isi.dan.reservas_svc.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.reservas_svc.dto.BloqueoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.PagoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaResponse;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReviewRequest;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ConflictoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion.ReservaSimple;
import edu.utn.frsf.isi.dan.reservas_svc.model.Hotel;
import edu.utn.frsf.isi.dan.reservas_svc.model.Pago;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Review;
import edu.utn.frsf.isi.dan.reservas_svc.model.Tarifa;
import edu.utn.frsf.isi.dan.reservas_svc.repository.ReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** "Hoy" = 2026-11-04 (reloj fijo). */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

  private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
  private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

  @Mock private ReservaRepository reservaRepository;
  @Mock private HabitacionService habitacionService;
  @Spy private ReservasMapper mapper = new ReservasMapper(reloj(HOY));
  @InjectMocks private ReservaService reservaService;

  private static Clock reloj(LocalDate fecha) {
    return Clock.fixed(fecha.atTime(12, 0).atZone(ZONA).toInstant(), ZONA);
  }

  private static Instant dia(int anio, int mes, int dia) {
    return LocalDate.of(anio, mes, dia).atStartOfDay(ZONA).toInstant();
  }

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reservaService, "clock", reloj(HOY));
    lenient()
        .when(reservaRepository.save(any(Reserva.class)))
        .thenAnswer(inv -> inv.getArgument(0));
  }

  private Habitacion habitacion(Double precioNoche, Boolean disponible) {
    return Habitacion.builder()
        .habitacionId(10L)
        .numero(101)
        .precioNoche(precioNoche)
        .disponible(disponible)
        .hotel(Hotel.builder().id(1).build())
        .build();
  }

  private ReservaRequest request(LocalDate in, LocalDate out) {
    return ReservaRequest.builder()
        .habitacionId(10L)
        .idHuesped(7)
        .checkIn(in)
        .checkOut(out)
        .build();
  }

  private Reserva reserva(EstadoReserva estado, double total, double... pagos) {
    List<Pago> lista = new ArrayList<>();
    for (double p : pagos) {
      lista.add(
          Pago.builder()
              .method("TARJETA")
              .amount(new Tarifa(p, "ARS"))
              .status("COMPLETED")
              .build());
    }
    return Reserva.builder()
        ._id("r1")
        .habitacionId(10L)
        .checkIn(dia(2026, 12, 10))
        .checkOut(dia(2026, 12, 15))
        .precioNoche(total / 5)
        .precioTotal(total)
        .estadoReserva(estado)
        .pago(lista)
        .build();
  }

  private void existeReserva(Reserva r) {
    when(reservaRepository.findById(r.get_id())).thenReturn(Optional.of(r));
  }

  // --------------------------------------------------------------------- crear

  @Test
  void crear_calculaPrecioTotalYQuedaReservada() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(true);

    ReservaResponse r =
        reservaService.crear(request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15)));

    assertEquals(EstadoReserva.RESERVADA, r.estado());
    assertEquals(15000.0, r.precioNoche());
    assertEquals(75000.0, r.precioTotal()); // 5 noches
    assertEquals(0.0, r.totalPagado());
    assertEquals(75000.0, r.saldo());
    assertEquals(LocalDate.of(2026, 12, 10), r.checkIn());
    assertEquals(LocalDate.of(2026, 12, 15), r.checkOut());
    assertEquals("7", r.idHuesped());
    assertEquals(1, r.hotelId());
    assertEquals(HOY.atTime(12, 0).atZone(ZONA).toInstant(), r.createdAt());
    assertNotNull(r.id());
  }

  @Test
  void crear_tomaElCupoConLasFechasALas0000DeBuenosAires_yGuardaLaCopiaEmbebida() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(true);

    ReservaResponse r =
        reservaService.crear(request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15)));

    ArgumentCaptor<ReservaSimple> simple = ArgumentCaptor.forClass(ReservaSimple.class);
    verify(habitacionService)
        .tomarCupo(eq(10L), simple.capture(), eq(dia(2026, 12, 10)), eq(dia(2026, 12, 15)));
    assertEquals(r.id(), simple.getValue().getReservaId());
    assertEquals(EstadoReserva.RESERVADA, simple.getValue().getEstadoReserva());
    assertEquals(75000.0, simple.getValue().getPrecioTotal());
    // 00:00 en Buenos Aires = 03:00 UTC
    assertEquals(Instant.parse("2026-12-10T03:00:00Z"), simple.getValue().getCheckIn());
  }

  @Test
  void crear_checkInDeHoyEsValido() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(true);

    assertDoesNotThrow(() -> reservaService.crear(request(HOY, HOY.plusDays(1))));
  }

  @Test
  void crear_conFechasInvalidas_lanzaIllegalArgumentSinTocarLaBase() {
    for (ReservaRequest r :
        List.of(
            request(LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 10)), // inversas
            request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10)), // mismo día
            request(HOY.minusDays(1), HOY.plusDays(2)))) { // check-in en el pasado
      assertThrows(IllegalArgumentException.class, () -> reservaService.crear(r));
    }
    verifyNoInteractions(habitacionService);
  }

  @Test
  void crear_habitacionInexistente_lanzaNoEncontrado() {
    when(habitacionService.findByHabitacionId(10L)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class,
        () ->
            reservaService.crear(request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15))));
  }

  @Test
  void crear_habitacionSinTarifaVigente_esConflicto() {
    when(habitacionService.findByHabitacionId(10L)).thenReturn(Optional.of(habitacion(null, true)));

    ConflictoException e =
        assertThrows(
            ConflictoException.class,
            () ->
                reservaService.crear(
                    request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15))));
    assertEquals("La habitación no tiene tarifa vigente", e.getMessage());
    verify(habitacionService, never()).tomarCupo(any(), any(), any(), any());
  }

  @Test
  void crear_habitacionNoDisponible_esConflicto() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, false)));

    assertThrows(
        ConflictoException.class,
        () ->
            reservaService.crear(request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15))));
  }

  @Test
  void crear_fechasOcupadas_esConflictoYNoGuardaLaReserva() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(false);

    ConflictoException e =
        assertThrows(
            ConflictoException.class,
            () ->
                reservaService.crear(
                    request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15))));
    assertTrue(e.getMessage().contains("no está disponible en esas fechas"));
    verify(reservaRepository, never()).save(any());
  }

  @Test
  void crear_siFallaElGuardado_liberaElCupoTomado() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(true);
    when(reservaRepository.save(any(Reserva.class))).thenThrow(new RuntimeException("mongo caído"));

    assertThrows(
        RuntimeException.class,
        () ->
            reservaService.crear(request(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15))));

    verify(habitacionService).liberarCupo(eq(10L), any(String.class));
  }

  // ------------------------------------------------------------------ bloqueos

  @Test
  void bloqueo_creaReservaBloqueadaSinHuespedNiPrecio() {
    when(habitacionService.findByHabitacionId(10L)).thenReturn(Optional.of(habitacion(null, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(true);

    ReservaResponse r =
        reservaService.crearBloqueo(
            BloqueoRequest.builder()
                .habitacionId(10L)
                .checkIn(LocalDate.of(2026, 12, 20))
                .checkOut(LocalDate.of(2026, 12, 22))
                .build());

    assertEquals(EstadoReserva.BLOQUEADA, r.estado());
    assertNull(r.idHuesped());
    assertNull(r.precioTotal());
    assertNull(r.precioNoche());
  }

  @Test
  void bloqueo_fechasOcupadas_esConflicto() {
    when(habitacionService.findByHabitacionId(10L))
        .thenReturn(Optional.of(habitacion(15000.0, true)));
    when(habitacionService.tomarCupo(eq(10L), any(ReservaSimple.class), any(), any()))
        .thenReturn(false);

    assertThrows(
        ConflictoException.class,
        () ->
            reservaService.crearBloqueo(
                BloqueoRequest.builder()
                    .habitacionId(10L)
                    .checkIn(LocalDate.of(2026, 12, 20))
                    .checkOut(LocalDate.of(2026, 12, 22))
                    .build()));
  }

  // --------------------------------------------------------------------- pagos

  private PagoRequest pago(double monto) {
    return PagoRequest.builder().method("TARJETA").monto(monto).transactionId("tx-1").build();
  }

  @Test
  void pago_elPrimeroConfirmaLaReservaYSincronizaLaHabitacion() {
    Reserva r = reserva(EstadoReserva.RESERVADA, 75000);
    existeReserva(r);

    ReservaResponse resp = reservaService.agregarPago("r1", pago(1000));

    assertEquals(EstadoReserva.CONFIRMADA, resp.estado());
    assertEquals(1000.0, resp.totalPagado());
    assertEquals(74000.0, resp.saldo());
    assertEquals("COMPLETED", resp.pagos().get(0).getStatus());
    assertEquals("ARS", resp.pagos().get(0).getAmount().getMoneda());
    verify(habitacionService).sincronizarEstado(10L, "r1", EstadoReserva.CONFIRMADA);
  }

  @Test
  void pago_siguientes_noCambianElEstadoNiSincronizan() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 1000);
    existeReserva(r);

    ReservaResponse resp = reservaService.agregarPago("r1", pago(2000));

    assertEquals(EstadoReserva.CONFIRMADA, resp.estado());
    assertEquals(3000.0, resp.totalPagado());
    verify(habitacionService, never()).sincronizarEstado(any(), any(), any());
  }

  @Test
  void pago_enAdeudadaSeAceptaYSigueAdeudada() {
    Reserva r = reserva(EstadoReserva.ADEUDADA, 75000, 1000);
    existeReserva(r);

    assertEquals(EstadoReserva.ADEUDADA, reservaService.agregarPago("r1", pago(74000)).estado());
  }

  @Test
  void pago_queExcedeElSaldo_oEnReservaCancelada_esConflicto() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 70000);
    existeReserva(r);
    assertThrows(ConflictoException.class, () -> reservaService.agregarPago("r1", pago(5000.01)));

    Reserva cancelada = reserva(EstadoReserva.CANCELADA, 75000);
    cancelada.set_id("r2");
    existeReserva(cancelada);
    assertThrows(ConflictoException.class, () -> reservaService.agregarPago("r2", pago(100)));
  }

  @Test
  void pago_reservaInexistente_lanzaNoEncontrado() {
    when(reservaRepository.findById("zzz")).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class, () -> reservaService.agregarPago("zzz", pago(10)));
  }

  // -------------------------------------------------------------------- estados

  @Test
  void cancelar_sinPagos_liberaLasFechasDeLaHabitacion() {
    existeReserva(reserva(EstadoReserva.RESERVADA, 75000));

    ReservaResponse r = reservaService.cambiarEstado("r1", EstadoReserva.CANCELADA);

    assertEquals(EstadoReserva.CANCELADA, r.estado());
    verify(habitacionService).liberarCupo(10L, "r1");
    verify(habitacionService, never()).sincronizarEstado(any(), any(), any());
  }

  @Test
  void cancelar_conPagos_esConflictoYNoTocaNada() {
    existeReserva(reserva(EstadoReserva.CONFIRMADA, 75000, 1000));

    assertThrows(
        ConflictoException.class,
        () -> reservaService.cambiarEstado("r1", EstadoReserva.CANCELADA));

    verify(reservaRepository, never()).save(any());
    verifyNoInteractions(habitacionService);
  }

  @Test
  void finalizar_completa_quedaFinalizadaYSincroniza() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 75000);
    r.setHostReview(Review.builder().rating(5).build());
    existeReserva(r);

    ReservaResponse resp = reservaService.cambiarEstado("r1", EstadoReserva.FINALIZADA);

    assertEquals(EstadoReserva.FINALIZADA, resp.estado());
    verify(habitacionService).sincronizarEstado(10L, "r1", EstadoReserva.FINALIZADA);
  }

  @Test
  void finalizar_sinReviewOPagoIncompleto_quedaAdeudada() {
    existeReserva(reserva(EstadoReserva.CONFIRMADA, 75000, 75000)); // sin review del dueño

    assertEquals(
        EstadoReserva.ADEUDADA,
        reservaService.cambiarEstado("r1", EstadoReserva.FINALIZADA).estado());
    verify(habitacionService).sincronizarEstado(10L, "r1", EstadoReserva.ADEUDADA);
  }

  @Test
  void finalizar_desdeReservadaSinPagos_quedaAdeudada() {
    Reserva r = reserva(EstadoReserva.RESERVADA, 75000);
    r.setHostReview(Review.builder().rating(5).build());
    existeReserva(r);

    assertEquals(
        EstadoReserva.ADEUDADA,
        reservaService.cambiarEstado("r1", EstadoReserva.FINALIZADA).estado());
  }

  @Test
  void estadoNoPermitidoPorPatch_esConflicto() {
    existeReserva(reserva(EstadoReserva.RESERVADA, 75000));

    assertThrows(
        ConflictoException.class,
        () -> reservaService.cambiarEstado("r1", EstadoReserva.BLOQUEADA));
    assertThrows(
        ConflictoException.class,
        () -> reservaService.cambiarEstado("r1", EstadoReserva.CONFIRMADA));
  }

  // ------------------------------------------------------------------- reviews

  private ReviewRequest review() {
    return ReviewRequest.builder().rating(4.5).comentario("Muy bien").build();
  }

  @Test
  void reviewCliente_antesDelCheckOut_esConflicto() {
    existeReserva(
        reserva(EstadoReserva.CONFIRMADA, 75000, 75000)); // checkOut 2026-12-15, hoy 11-04

    assertThrows(ConflictoException.class, () -> reservaService.reviewCliente("r1", review()));
  }

  @Test
  void reviewCliente_despuesDelCheckOut_seGuarda() {
    ReflectionTestUtils.setField(reservaService, "clock", reloj(LocalDate.of(2027, 1, 15)));
    existeReserva(reserva(EstadoReserva.CONFIRMADA, 75000, 75000));

    ReservaResponse r = reservaService.reviewCliente("r1", review());

    assertEquals(4.5, r.clientReview().getRating());
    assertEquals("Muy bien", r.clientReview().getComment());
    assertEquals(
        LocalDate.of(2027, 1, 15).atTime(12, 0).atZone(ZONA).toInstant(),
        r.clientReview().getCreatedAt());
  }

  @Test
  void reviewCliente_elMismoDiaDelCheckOut_yaVale() {
    ReflectionTestUtils.setField(reservaService, "clock", reloj(LocalDate.of(2026, 12, 15)));
    existeReserva(reserva(EstadoReserva.CONFIRMADA, 75000, 75000));

    assertDoesNotThrow(() -> reservaService.reviewCliente("r1", review()));
  }

  @Test
  void reviewDueno_seGuardaEnHostReview_yPermiteFinalizar() {
    Reserva r = reserva(EstadoReserva.CONFIRMADA, 75000, 75000);
    existeReserva(r);

    ReservaResponse resp = reservaService.reviewDueno("r1", review());

    assertEquals(4.5, resp.hostReview().getRating());
    assertNull(resp.clientReview());
    assertEquals(
        EstadoReserva.FINALIZADA,
        reservaService.cambiarEstado("r1", EstadoReserva.FINALIZADA).estado());
  }

  // ----------------------------------------------------------------- consultas

  @Test
  void listarPorHuesped_filtraPorIdDeUsuario() {
    when(reservaRepository.findByHuesped_IdUsuario("7"))
        .thenReturn(List.of(reserva(EstadoReserva.RESERVADA, 75000)));

    assertEquals(1, reservaService.listarPorHuesped(7).size());
  }

  @Test
  void obtener_inexistente_lanzaNoEncontrado() {
    when(reservaRepository.findById("zzz")).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> reservaService.obtener("zzz"));
  }
}
