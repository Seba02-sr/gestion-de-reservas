package edu.utn.frsf.isi.dan.gestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaVigenteResponse;
import edu.utn.frsf.isi.dan.gestion.service.HabitacionService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HabitacionController.class)
class HabitacionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HabitacionService habitacionService;

  private static final String BODY_VALIDO =
      "{\"numero\":101,\"piso\":2,\"idTipoHabitacion\":3,\"idHotel\":1}";

  private static HabitacionResponse response() {
    return HabitacionResponse.builder()
        .id(10)
        .numero(101)
        .piso(2)
        .disponible(true)
        .idTipoHabitacion(3)
        .tipoHabitacion("DOBLE")
        .idHotel(1)
        .build();
  }

  @Test
  void crear_conDatosValidos_devuelve201() throws Exception {
    when(habitacionService.crear(any(HabitacionRequest.class))).thenReturn(response());

    mockMvc
        .perform(post("/habitaciones").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(10))
        .andExpect(jsonPath("$.disponible").value(true))
        .andExpect(jsonPath("$.idHotel").value(1));
  }

  @Test
  void crear_conValidacionesInvalidas_devuelve400() throws Exception {
    String[] invalidos = {
      "{\"numero\":0,\"piso\":2,\"idTipoHabitacion\":3,\"idHotel\":1}",
      "{\"numero\":-5,\"piso\":2,\"idTipoHabitacion\":3,\"idHotel\":1}",
      "{\"piso\":2,\"idTipoHabitacion\":3,\"idHotel\":1}",
      "{\"numero\":101,\"piso\":0,\"idTipoHabitacion\":3,\"idHotel\":1}",
      "{\"numero\":101,\"idTipoHabitacion\":3,\"idHotel\":1}",
      "{\"numero\":101,\"piso\":2,\"idHotel\":1}",
      "{\"numero\":101,\"piso\":2,\"idTipoHabitacion\":3}"
    };
    for (String body : invalidos) {
      mockMvc
          .perform(post("/habitaciones").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verify(habitacionService, never()).crear(any());
  }

  @Test
  void crear_hotelInexistente_devuelve404() throws Exception {
    when(habitacionService.crear(any())).thenThrow(new EntityNotFoundException("No existe hotel"));

    mockMvc
        .perform(post("/habitaciones").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isNotFound());
  }

  @Test
  void crear_numeroRepetidoOHotelCerrado_devuelve409() throws Exception {
    when(habitacionService.crear(any()))
        .thenThrow(new IllegalStateException("Ya existe la habitación número 101 en el hotel 1"));

    mockMvc
        .perform(post("/habitaciones").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void actualizar_numeroRepetido_devuelve409() throws Exception {
    when(habitacionService.actualizar(eq(10), any()))
        .thenThrow(new IllegalStateException("Ya existe la habitación número 101 en el hotel 1"));

    mockMvc
        .perform(
            put("/habitaciones/10").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isConflict());
  }

  @Test
  void actualizar_conDatosValidos_devuelve200() throws Exception {
    when(habitacionService.actualizar(eq(10), any(HabitacionRequest.class))).thenReturn(response());

    mockMvc
        .perform(
            put("/habitaciones/10").contentType(MediaType.APPLICATION_JSON).content(BODY_VALIDO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numero").value(101));
  }

  @Test
  void actualizar_conPisoNegativo_devuelve400() throws Exception {
    mockMvc
        .perform(
            put("/habitaciones/10")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"numero\":101,\"piso\":-1,\"idTipoHabitacion\":3,\"idHotel\":1}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void obtener_devuelveLaHabitacionComoDto() throws Exception {
    when(habitacionService.obtener(10)).thenReturn(response());

    mockMvc
        .perform(get("/habitaciones/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipoHabitacion").value("DOBLE"))
        .andExpect(jsonPath("$.hotel").doesNotExist());
  }

  @Test
  void obtener_inexistente_devuelve404() throws Exception {
    when(habitacionService.obtener(99)).thenThrow(new EntityNotFoundException("No existe"));

    mockMvc.perform(get("/habitaciones/99")).andExpect(status().isNotFound());
  }

  @Test
  void buscar_pasaLosFiltrosOpcionalesAlService() throws Exception {
    when(habitacionService.buscar(2, 3, 1000.0, 20000.0)).thenReturn(List.of(response()));

    mockMvc
        .perform(
            get("/habitaciones")
                .param("cantidadHuespedes", "2")
                .param("tipoHabitacionId", "3")
                .param("precioMin", "1000")
                .param("precioMax", "20000"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(10));
  }

  @Test
  void buscar_sinFiltros_pasaNulos() throws Exception {
    when(habitacionService.buscar(null, null, null, null)).thenReturn(List.of());

    mockMvc.perform(get("/habitaciones")).andExpect(status().isOk());
    verify(habitacionService).buscar(null, null, null, null);
  }

  @Test
  void buscar_precioMinMayorQueMax_devuelve400() throws Exception {
    when(habitacionService.buscar(any(), any(), eq(5000.0), eq(1000.0)))
        .thenThrow(new IllegalArgumentException("precioMin no puede ser mayor que precioMax"));

    mockMvc
        .perform(get("/habitaciones").param("precioMin", "5000").param("precioMax", "1000"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void tarifaVigente_devuelveLaTarifa() throws Exception {
    when(habitacionService.obtenerTarifaVigente(10))
        .thenReturn(
            TarifaVigenteResponse.builder()
                .id(7)
                .idTipoHabitacion(3)
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .precioNoche(15000.0)
                .build());

    mockMvc
        .perform(get("/habitaciones/10/tarifa-vigente"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.precioNoche").value(15000.0))
        .andExpect(jsonPath("$.fechaFin").doesNotExist());
  }

  @Test
  void tarifaVigente_sinTarifa_devuelve404() throws Exception {
    when(habitacionService.obtenerTarifaVigente(10))
        .thenThrow(new EntityNotFoundException("No hay tarifa vigente"));

    mockMvc.perform(get("/habitaciones/10/tarifa-vigente")).andExpect(status().isNotFound());
  }

  @Test
  void disponible_devuelveUnBoolean() throws Exception {
    when(habitacionService.estaDisponible(10)).thenReturn(true);
    when(habitacionService.estaDisponible(11)).thenReturn(false);

    mockMvc.perform(get("/habitaciones/10/disponible")).andExpect(content().string("true"));
    mockMvc.perform(get("/habitaciones/11/disponible")).andExpect(content().string("false"));
  }

  @Test
  void eliminar_devuelve204() throws Exception {
    mockMvc.perform(delete("/habitaciones/10")).andExpect(status().isNoContent());
    verify(habitacionService).eliminar(10);
  }
}
