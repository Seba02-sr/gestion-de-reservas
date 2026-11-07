package edu.utn.frsf.isi.dan.gestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.service.TipoHabitacionService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TipoHabitacionController.class)
class TipoHabitacionControllerTest {

  private static final String VALIDO =
      "{\"nombre\":\"SUITE\",\"descripcion\":\"Suite con living\",\"capacidad\":4}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TipoHabitacionService tipoHabitacionService;

  private static TipoHabitacionResponse response() {
    return new TipoHabitacionResponse(10, "SUITE", "Suite con living", 4);
  }

  @Test
  void crear_devuelve201() throws Exception {
    when(tipoHabitacionService.crear(any(TipoHabitacionRequest.class))).thenReturn(response());

    mockMvc
        .perform(post("/tipos-habitacion").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10));
  }

  @Test
  void crear_conDatosInvalidos_devuelve400() throws Exception {
    String[] invalidos = {
      "{\"descripcion\":\"x\",\"capacidad\":2}",
      "{\"nombre\":\"\",\"descripcion\":\"x\",\"capacidad\":2}",
      "{\"nombre\":\"A\",\"capacidad\":2}",
      "{\"nombre\":\"A\",\"descripcion\":\" \",\"capacidad\":2}",
      "{\"nombre\":\"A\",\"descripcion\":\"x\"}",
      "{\"nombre\":\"A\",\"descripcion\":\"x\",\"capacidad\":0}",
      "{\"nombre\":\"A\",\"descripcion\":\"x\",\"capacidad\":-1}"
    };
    for (String body : invalidos) {
      mockMvc
          .perform(post("/tipos-habitacion").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verify(tipoHabitacionService, never()).crear(any());
  }

  @Test
  void actualizar_validaIgualQueElAlta() throws Exception {
    when(tipoHabitacionService.actualizar(eq(10), any())).thenReturn(response());

    mockMvc
        .perform(
            put("/tipos-habitacion/10").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put("/tipos-habitacion/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"A\",\"descripcion\":\"x\",\"capacidad\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obtener_listar_yNoEncontrado() throws Exception {
    when(tipoHabitacionService.obtener(10)).thenReturn(response());
    when(tipoHabitacionService.obtener(99)).thenThrow(new EntityNotFoundException("No existe"));
    when(tipoHabitacionService.listar()).thenReturn(List.of(response()));

    mockMvc.perform(get("/tipos-habitacion/10")).andExpect(jsonPath("$.capacidad").value(4));
    mockMvc.perform(get("/tipos-habitacion/99")).andExpect(status().isNotFound());
    mockMvc.perform(get("/tipos-habitacion")).andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void eliminar_devuelve204_oConflictoSiEstaEnUso() throws Exception {
    mockMvc.perform(delete("/tipos-habitacion/10")).andExpect(status().isNoContent());

    doThrow(new IllegalStateException("tiene habitaciones"))
        .when(tipoHabitacionService)
        .eliminar(11);
    mockMvc.perform(delete("/tipos-habitacion/11")).andExpect(status().isConflict());
  }
}
