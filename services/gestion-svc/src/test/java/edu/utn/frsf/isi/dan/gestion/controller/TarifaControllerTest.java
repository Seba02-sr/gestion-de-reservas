package edu.utn.frsf.isi.dan.gestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.utn.frsf.isi.dan.gestion.dto.TarifaRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaResponse;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TarifaController.class)
class TarifaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TarifaService tarifaService;

  private static TarifaResponse response() {
    return TarifaResponse.builder()
        .id(5)
        .nombre("Base")
        .idTipoHabitacion(3)
        .fechaInicio(LocalDate.of(2026, 11, 4))
        .precioNoche(12000.0)
        .especial(false)
        .build();
  }

  @Test
  void crear_tarifaComun_devuelve201() throws Exception {
    when(tarifaService.crear(any(TarifaRequest.class))).thenReturn(response());

    mockMvc
        .perform(
            post("/tarifas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Base\",\"idTipoHabitacion\":3,\"precioNoche\":12000}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.fechaFin").doesNotExist());
  }

  @Test
  void crear_conFechas_pasaLasFechasAlService() throws Exception {
    when(tarifaService.crear(any(TarifaRequest.class))).thenReturn(response());

    mockMvc
        .perform(
            post("/tarifas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Promo\",\"idTipoHabitacion\":3,\"precioNoche\":9000,"
                        + "\"fechaInicio\":\"2026-12-20\",\"fechaFin\":\"2027-01-05\"}"))
        .andExpect(status().isCreated());

    verify(tarifaService)
        .crear(
            TarifaRequest.builder()
                .nombre("Promo")
                .idTipoHabitacion(3)
                .precioNoche(9000.0)
                .fechaInicio(LocalDate.of(2026, 12, 20))
                .fechaFin(LocalDate.of(2027, 1, 5))
                .build());
  }

  @Test
  void crear_conDatosInvalidos_devuelve400() throws Exception {
    String[] invalidos = {
      "{\"idTipoHabitacion\":3,\"precioNoche\":12000}",
      "{\"nombre\":\" \",\"idTipoHabitacion\":3,\"precioNoche\":12000}",
      "{\"nombre\":\"Base\",\"precioNoche\":12000}",
      "{\"nombre\":\"Base\",\"idTipoHabitacion\":3}",
      "{\"nombre\":\"Base\",\"idTipoHabitacion\":3,\"precioNoche\":0}",
      "{\"nombre\":\"Base\",\"idTipoHabitacion\":3,\"precioNoche\":-5}",
      "{\"nombre\":\"Base\",\"idTipoHabitacion\":3,\"precioNoche\":10,\"fechaInicio\":\"no-es-fecha\"}"
    };
    for (String body : invalidos) {
      mockMvc
          .perform(post("/tarifas").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verify(tarifaService, never()).crear(any());
  }

  @Test
  void crear_unaSolaFecha_devuelve400() throws Exception {
    when(tarifaService.crear(any()))
        .thenThrow(new IllegalArgumentException("Hay que informar fechaInicio y fechaFin"));

    mockMvc
        .perform(
            post("/tarifas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Promo\",\"idTipoHabitacion\":3,\"precioNoche\":9000,\"fechaInicio\":\"2026-12-20\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void crear_promoSuperpuesta_devuelve409() throws Exception {
    when(tarifaService.crear(any()))
        .thenThrow(new IllegalStateException("La promoción se superpone con otra"));

    mockMvc
        .perform(
            post("/tarifas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Promo\",\"idTipoHabitacion\":3,\"precioNoche\":9000,"
                        + "\"fechaInicio\":\"2026-12-20\",\"fechaFin\":\"2027-01-05\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void crear_tipoInexistente_devuelve404() throws Exception {
    when(tarifaService.crear(any())).thenThrow(new EntityNotFoundException("No existe el tipo"));

    mockMvc
        .perform(
            post("/tarifas")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Base\",\"idTipoHabitacion\":99,\"precioNoche\":12000}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void eliminar_devuelve204() throws Exception {
    mockMvc.perform(delete("/tarifas/5")).andExpect(status().isNoContent());
    verify(tarifaService).eliminar(5);
  }

  @Test
  void eliminar_laUnicaTarifa_devuelve409() throws Exception {
    doThrow(new IllegalStateException("es la única")).when(tarifaService).eliminar(5);

    mockMvc.perform(delete("/tarifas/5")).andExpect(status().isConflict());
  }

  @Test
  void eliminar_inexistente_devuelve404() throws Exception {
    doThrow(new EntityNotFoundException("No existe")).when(tarifaService).eliminar(99);

    mockMvc.perform(delete("/tarifas/99")).andExpect(status().isNotFound());
  }

  @Test
  void noHayModificacion_putDevuelve405() throws Exception {
    mockMvc
        .perform(
            put("/tarifas/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Base\",\"idTipoHabitacion\":3,\"precioNoche\":12000}"))
        .andExpect(status().isMethodNotAllowed());
    verifyNoInteractions(tarifaService);
  }

  @Test
  void listar_conYSinFiltroDeTipo() throws Exception {
    when(tarifaService.listar(null)).thenReturn(List.of(response()));
    when(tarifaService.listar(3)).thenReturn(List.of(response(), response()));

    mockMvc.perform(get("/tarifas")).andExpect(jsonPath("$.length()").value(1));
    mockMvc
        .perform(get("/tarifas").param("idTipoHabitacion", "3"))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void vigente_conFecha_usaLaFechaPedida() throws Exception {
    when(tarifaService.vigente(3, LocalDate.of(2026, 12, 25))).thenReturn(response());

    mockMvc
        .perform(
            get("/tarifas/vigente").param("idTipoHabitacion", "3").param("fecha", "2026-12-25"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.precioNoche").value(12000.0));
  }

  @Test
  void vigente_sinFecha_pasaNull() throws Exception {
    when(tarifaService.vigente(3, null)).thenReturn(response());

    mockMvc
        .perform(get("/tarifas/vigente").param("idTipoHabitacion", "3"))
        .andExpect(status().isOk());
  }

  @Test
  void vigente_sinTarifa_devuelve404() throws Exception {
    when(tarifaService.vigente(3, null)).thenThrow(new EntityNotFoundException("No hay tarifa"));

    mockMvc
        .perform(get("/tarifas/vigente").param("idTipoHabitacion", "3"))
        .andExpect(status().isNotFound());
  }

  @Test
  void obtener_porId() throws Exception {
    when(tarifaService.obtener(5)).thenReturn(response());

    mockMvc.perform(get("/tarifas/5")).andExpect(jsonPath("$.nombre").value("Base"));
  }
}
