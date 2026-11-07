package edu.utn.frsf.isi.dan.reservas_svc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaResponse;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ConflictoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ServicioNoDisponibleException;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.service.ReservaService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservaController.class)
class ReservaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReservaService reservaService;

  private static final String BODY_VALIDO =
      "{\"habitacionId\":10,\"idHuesped\":7,\"checkIn\":\"2026-12-10\",\"checkOut\":\"2026-12-15\"}";

  private static ReservaResponse response(EstadoReserva estado) {
    return ReservaResponse.builder()
        .id("r1")
        .habitacionId(10L)
        .idHuesped("7")
        .checkIn(LocalDate.of(2026, 12, 10))
        .checkOut(LocalDate.of(2026, 12, 15))
        .precioNoche(15000.0)
        .precioTotal(75000.0)
        .estado(estado)
        .build();
  }

  @Test
  void crear_devuelve201() throws Exception {
    when(reservaService.crear(any(ReservaRequest.class)))
        .thenReturn(response(EstadoReserva.RESERVADA));

    mockMvc
        .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value("r1"))
        .andExpect(jsonPath("$.estado").value("RESERVADA"))
        .andExpect(jsonPath("$.checkIn").value("2026-12-10"));
  }

  @Test
  void crear_conHuespedInexistente_devuelve404ConMensaje() throws Exception {
    when(reservaService.crear(any(ReservaRequest.class)))
        .thenThrow(new RecursoNoEncontradoException("No existe el huésped 7"));

    mockMvc
        .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("No existe el huésped 7"));
  }

  @Test
  void crear_conServicioCaido_devuelve503ConMensajeClaro_noUn500() throws Exception {
    when(reservaService.crear(any(ReservaRequest.class)))
        .thenThrow(
            new ServicioNoDisponibleException(
                "No se pudo validar el huésped 7: user-svc no está disponible. Intente nuevamente.",
                null));

    mockMvc
        .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("user-svc")));
  }

  @Test
  void crear_conDatosInvalidos_devuelve400() throws Exception {
    String[] invalidos = {
      "{\"idHuesped\":7,\"checkIn\":\"2026-12-10\",\"checkOut\":\"2026-12-15\"}",
      "{\"habitacionId\":10,\"checkIn\":\"2026-12-10\",\"checkOut\":\"2026-12-15\"}",
      "{\"habitacionId\":10,\"idHuesped\":7,\"checkOut\":\"2026-12-15\"}",
      "{\"habitacionId\":10,\"idHuesped\":7,\"checkIn\":\"2026-12-10\"}",
      "{\"habitacionId\":-1,\"idHuesped\":7,\"checkIn\":\"2026-12-10\",\"checkOut\":\"2026-12-15\"}",
      "{\"habitacionId\":10,\"idHuesped\":7,\"checkIn\":\"10/12/2026\",\"checkOut\":\"2026-12-15\"}",
      "{esto no es json"
    };
    for (String body : invalidos) {
      mockMvc
          .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verify(reservaService, never()).crear(any());
  }

  @Test
  void crear_fechasInvalidasSegunElNegocio_devuelve400() throws Exception {
    when(reservaService.crear(any()))
        .thenThrow(new IllegalArgumentException("checkIn debe ser anterior a checkOut"));

    mockMvc
        .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void crear_habitacionInexistente_devuelve404() throws Exception {
    when(reservaService.crear(any()))
        .thenThrow(new RecursoNoEncontradoException("No existe la habitación 10"));

    mockMvc
        .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("No existe la habitación 10"));
  }

  @Test
  void crear_sinTarifaOFechasOcupadas_devuelve409() throws Exception {
    when(reservaService.crear(any()))
        .thenThrow(new ConflictoException("La habitación no tiene tarifa vigente"));

    mockMvc
        .perform(post("/reservas").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("La habitación no tiene tarifa vigente"));
  }

  @Test
  void listarYObtener() throws Exception {
    when(reservaService.listarPorHuesped(7)).thenReturn(List.of(response(EstadoReserva.RESERVADA)));
    when(reservaService.obtener("r1")).thenReturn(response(EstadoReserva.CONFIRMADA));
    when(reservaService.obtener("zzz")).thenThrow(new RecursoNoEncontradoException("No existe"));

    mockMvc
        .perform(get("/reservas").param("idHuesped", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mockMvc.perform(get("/reservas/r1")).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    mockMvc.perform(get("/reservas/zzz")).andExpect(status().isNotFound());
    mockMvc.perform(get("/reservas").param("idHuesped", "abc")).andExpect(status().isBadRequest());
  }

  @Test
  void pagar_validaElBody_yDevuelve201() throws Exception {
    when(reservaService.agregarPago(eq("r1"), any()))
        .thenReturn(response(EstadoReserva.CONFIRMADA));

    mockMvc
        .perform(
            post("/reservas/r1/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"TARJETA\",\"monto\":1000}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    for (String invalido :
        List.of(
            "{\"monto\":1000}",
            "{\"method\":\"TARJETA\"}",
            "{\"method\":\"TARJETA\",\"monto\":0}",
            "{\"method\":\"TARJETA\",\"monto\":-5}")) {
      mockMvc
          .perform(
              post("/reservas/r1/pagos").contentType(MediaType.APPLICATION_JSON).content(invalido))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void pagar_montoMayorAlSaldo_devuelve409() throws Exception {
    when(reservaService.agregarPago(eq("r1"), any()))
        .thenThrow(new ConflictoException("El pago excede el saldo pendiente"));

    mockMvc
        .perform(
            post("/reservas/r1/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"TARJETA\",\"monto\":999999}"))
        .andExpect(status().isConflict());
  }

  @Test
  void cambiarEstado_aceptaElEstadoYTraduceLosConflictos() throws Exception {
    when(reservaService.cambiarEstado("r1", EstadoReserva.FINALIZADA))
        .thenReturn(response(EstadoReserva.ADEUDADA));
    when(reservaService.cambiarEstado("r1", EstadoReserva.CANCELADA))
        .thenThrow(new ConflictoException("No se puede cancelar una reserva que tiene pagos"));

    mockMvc
        .perform(
            patch("/reservas/r1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"FINALIZADA\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estado").value("ADEUDADA"));
    mockMvc
        .perform(
            patch("/reservas/r1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"CANCELADA\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void cambiarEstado_conEstadoInexistenteOSinEstado_devuelve400() throws Exception {
    for (String body : List.of("{\"estado\":\"VOLANDO\"}", "{}")) {
      mockMvc
          .perform(
              patch("/reservas/r1/estado").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void reviews_validanElRatingDe1a5() throws Exception {
    when(reservaService.reviewCliente(eq("r1"), any()))
        .thenReturn(response(EstadoReserva.FINALIZADA));
    when(reservaService.reviewDueno(eq("r1"), any()))
        .thenReturn(response(EstadoReserva.CONFIRMADA));

    for (String ruta : List.of("review-cliente", "review-dueno")) {
      mockMvc
          .perform(
              put("/reservas/r1/" + ruta)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"rating\":4.5,\"comentario\":\"Muy bien\"}"))
          .andExpect(status().isOk());
      for (String malo :
          List.of("{\"rating\":0}", "{\"rating\":5.1}", "{\"comentario\":\"sin rating\"}")) {
        mockMvc
            .perform(
                put("/reservas/r1/" + ruta).contentType(MediaType.APPLICATION_JSON).content(malo))
            .andExpect(status().isBadRequest());
      }
    }
  }

  @Test
  void reviewCliente_antesDelCheckOut_devuelve409() throws Exception {
    when(reservaService.reviewCliente(eq("r1"), any()))
        .thenThrow(new ConflictoException("Solo se puede calificar después del check-out"));

    mockMvc
        .perform(
            put("/reservas/r1/review-cliente")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4}"))
        .andExpect(status().isConflict());
  }

  @Test
  void bloqueo_devuelve201() throws Exception {
    when(reservaService.crearBloqueo(any())).thenReturn(response(EstadoReserva.BLOQUEADA));

    mockMvc
        .perform(
            post("/reservas/bloqueos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"habitacionId\":10,\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-22\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.estado").value("BLOQUEADA"));
  }

  @Test
  void yaNoExisteElCrudGenerico_deleteYPutSobreReservaDan405() throws Exception {
    mockMvc.perform(delete("/reservas/r1")).andExpect(status().isMethodNotAllowed());
    mockMvc
        .perform(put("/reservas/r1").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed());
    verifyNoInteractions(reservaService);
  }
}
