package edu.utn.frsf.isi.dan.reservas_svc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.utn.frsf.isi.dan.reservas_svc.config.TiempoConfig;
import edu.utn.frsf.isi.dan.reservas_svc.dto.BusquedaHabitacionesRequest;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Hotel;
import edu.utn.frsf.isi.dan.reservas_svc.service.HabitacionService;
import edu.utn.frsf.isi.dan.reservas_svc.service.ReservasMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HabitacionController.class)
@Import({ReservasMapper.class, TiempoConfig.class})
class HabitacionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HabitacionService habitacionService;

  private static Habitacion habitacion() {
    return Habitacion.builder()
        .habitacionId(10L)
        .numero(101)
        .piso(2)
        .capacidad(2)
        .precioNoche(15000.0)
        .amenities(List.of("WIFI"))
        .hotel(
            Hotel.builder()
                .id(1)
                .nombre("Hotel Castelar")
                .categoria(4)
                .ubicacion(new GeoJsonPoint(-60.70, -31.63))
                .build())
        .build();
  }

  @Test
  void listar_devuelveDtosConLatitudYLongitudPlanas_sinGeoJson() throws Exception {
    when(habitacionService.findAll()).thenReturn(List.of(habitacion()));

    mockMvc
        .perform(get("/habitaciones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].habitacionId").value(10))
        .andExpect(jsonPath("$[0].disponible").value(true))
        .andExpect(jsonPath("$[0].hotel.latitud").value(-31.63))
        .andExpect(jsonPath("$[0].hotel.longitud").value(-60.70))
        .andExpect(jsonPath("$[0].hotel.ubicacion").doesNotExist());
  }

  @Test
  void obtenerPorIdDeGestion() throws Exception {
    when(habitacionService.findByHabitacionId(10L)).thenReturn(Optional.of(habitacion()));
    when(habitacionService.findByHabitacionId(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/habitaciones/10")).andExpect(jsonPath("$.numero").value(101));
    mockMvc.perform(get("/habitaciones/99")).andExpect(status().isNotFound());
    mockMvc.perform(get("/habitaciones/abc")).andExpect(status().isBadRequest());
  }

  @Test
  void disponibles_bindeaTodosLosFiltros() throws Exception {
    when(habitacionService.buscarDisponibles(any())).thenReturn(List.of(habitacion()));

    mockMvc
        .perform(
            get("/habitaciones/disponibles")
                .param("checkIn", "2026-12-10")
                .param("checkOut", "2026-12-15")
                .param("cantidadHuespedes", "2")
                .param("precioMin", "10000")
                .param("precioMax", "20000")
                .param("estrellas", "3")
                .param("amenities", "WIFI,PILETA")
                .param("lat", "-31.63")
                .param("lng", "-60.70")
                .param("radioKm", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    ArgumentCaptor<BusquedaHabitacionesRequest> filtros =
        ArgumentCaptor.forClass(BusquedaHabitacionesRequest.class);
    verify(habitacionService).buscarDisponibles(filtros.capture());
    BusquedaHabitacionesRequest f = filtros.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 12, 10), f.checkIn());
    org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2026, 12, 15), f.checkOut());
    org.junit.jupiter.api.Assertions.assertEquals(2, f.cantidadHuespedes());
    org.junit.jupiter.api.Assertions.assertEquals(List.of("WIFI", "PILETA"), f.amenities());
    org.junit.jupiter.api.Assertions.assertEquals(-31.63, f.lat());
    org.junit.jupiter.api.Assertions.assertEquals(-60.70, f.lng());
    org.junit.jupiter.api.Assertions.assertEquals(5.0, f.radioKm());
  }

  @Test
  void disponibles_sinFiltros_devuelveLoQueDiceElServicio() throws Exception {
    when(habitacionService.buscarDisponibles(any())).thenReturn(List.of());

    mockMvc
        .perform(get("/habitaciones/disponibles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void disponibles_conFiltrosInvalidos_devuelve400() throws Exception {
    for (String[] p :
        new String[][] {
          {"cantidadHuespedes", "0"},
          {"precioMin", "-1"},
          {"lat", "95"},
          {"lng", "-181"},
          {"radioKm", "0"},
          {"checkIn", "10-12-2026"}
        }) {
      mockMvc
          .perform(get("/habitaciones/disponibles").param(p[0], p[1]))
          .andExpect(status().isBadRequest());
    }
    verify(habitacionService, never()).buscarDisponibles(any());
  }

  @Test
  void disponibles_combinacionInvalidaDelNegocio_devuelve400() throws Exception {
    when(habitacionService.buscarDisponibles(any()))
        .thenThrow(new IllegalArgumentException("lat, lng y radioKm se informan juntos"));

    mockMvc
        .perform(get("/habitaciones/disponibles").param("lat", "-31.6"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void lasHabitacionesNoSeCreanModificanNiBorranPorHttp_405() throws Exception {
    mockMvc
        .perform(post("/habitaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed());
    mockMvc
        .perform(put("/habitaciones/10").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isMethodNotAllowed());
    mockMvc.perform(delete("/habitaciones/10")).andExpect(status().isMethodNotAllowed());
    verifyNoInteractions(habitacionService);
  }
}
