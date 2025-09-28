package edu.utn.frsf.isi.dan.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.service.HuespedService;
import edu.utn.frsf.isi.dan.user.service.TarjetaCreditoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HuespedController.class)
@DisplayName("Tests del Controlador de Huespedes")
class HuespedControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private HuespedService huespedService;
  @MockitoBean private TarjetaCreditoService tarjetaCreditoService;
  @Autowired private ObjectMapper objectMapper;

  private HuespedRequest request;
  private HuespedResponse response;

  @BeforeEach
  void setUp() {
    request =
        HuespedRequest.builder()
            .nombre("Juan")
            .email("juan@example.com")
            .telefono("123")
            .dni("1111")
            .username("juan")
            .password("secret")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .tarjetasCredito(
                List.of(
                    TarjetaCreditoRequest.builder()
                        .numero("4111111111111111")
                        .nombreTitular("Juan Perez")
                        .fechaVencimiento("12/25")
                        .cvc("123")
                        .esPrincipal(true)
                        .idBanco(1)
                        .build()))
            .build();

    response =
        HuespedResponse.builder()
            .id(1)
            .nombre("Juan")
            .dni("1111")
            .tarjetaCredito(
                List.of(
                    TarjetaCreditoResponse.builder()
                        .id(10)
                        .numero("4111111111111111")
                        .esPrincipal(true)
                        .idBanco(1)
                        .build()))
            .build();
  }

  @Test
  void listar_DebeRetornar200() throws Exception {
    when(huespedService.getAllHuespedes()).thenReturn(List.of(response));
    mockMvc.perform(get("/huespedes")).andExpect(status().isOk());
    verify(huespedService, times(1)).getAllHuespedes();
  }

  @Test
  void obtenerPorId_DebeRetornar200() throws Exception {
    when(huespedService.getHuespedById(1)).thenReturn(response);
    mockMvc
        .perform(get("/huespedes/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
    verify(huespedService, times(1)).getHuespedById(1);
  }

  @Test
  void actualizar_DebeRetornar200() throws Exception {
    when(huespedService.actualizarHuesped(eq(1), any(HuespedRequest.class))).thenReturn(response);
    mockMvc
        .perform(
            put("/huespedes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
    verify(huespedService, times(1)).actualizarHuesped(eq(1), any(HuespedRequest.class));
  }

  @Test
  void eliminar_DebeRetornar204() throws Exception {
    doNothing().when(huespedService).eliminarHuesped(1);
    mockMvc.perform(delete("/huespedes/1")).andExpect(status().isNoContent());
    verify(huespedService, times(1)).eliminarHuesped(1);
  }

  @Test
  void buscarPorNombre_DebeRetornar200() throws Exception {
    when(huespedService.buscarPorNombre("Ju")).thenReturn(List.of(response));
    mockMvc.perform(get("/huespedes/buscar").param("nombre", "Ju")).andExpect(status().isOk());
    verify(huespedService, times(1)).buscarPorNombre("Ju");
  }

  @Test
  void buscarPorDni_DebeRetornar200() throws Exception {
    when(huespedService.buscarPorDni("1111")).thenReturn(response);
    mockMvc.perform(get("/huespedes/dni/1111")).andExpect(status().isOk());
    verify(huespedService, times(1)).buscarPorDni("1111");
  }
}
