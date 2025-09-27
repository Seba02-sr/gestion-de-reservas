package edu.utn.frsf.isi.dan.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoResponse;
import edu.utn.frsf.isi.dan.user.service.TarjetaCreditoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TarjetaCreditoController.class)
@DisplayName("Tests del Controlador de Tarjetas")
class TarjetaCreditoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TarjetaCreditoService tarjetaCreditoService;

  @Autowired private ObjectMapper objectMapper;

  private TarjetaCreditoResponse response;
  private TarjetaCreditoRequest request;

  @BeforeEach
  void setUp() {
    response =
        TarjetaCreditoResponse.builder()
            .id(1)
            .numero("4111111111111111")
            .nombreTitular("Juan Perez")
            .cvc("123")
            .fechaVencimiento("12/25")
            .esPrincipal(false)
            .idBanco(1)
            .build();
    request =
        TarjetaCreditoRequest.builder()
            .numero("4111111111111111")
            .nombreTitular("Juan Perez")
            .cvc("123")
            .fechaVencimiento("12/25")
            .esPrincipal(false)
            .idBanco(1)
            .build();
  }

  @Test
  void getTarjetaPorId_DebeRetornar200() throws Exception {
    when(tarjetaCreditoService.getTarjetaById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/tarjetas/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.numero").value("4111111111111111"));

    verify(tarjetaCreditoService, times(1)).getTarjetaById(1L);
  }

  @Test
  void updateTarjeta_DebeRetornar200() throws Exception {
    when(tarjetaCreditoService.updateTarjetaCredito(eq(1L), any(TarjetaCreditoRequest.class)))
        .thenReturn(response);
    mockMvc
        .perform(
            put("/tarjetas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
    verify(tarjetaCreditoService, times(1))
        .updateTarjetaCredito(eq(1L), any(TarjetaCreditoRequest.class));
  }

  @Test
  void setPrincipal_DebeRetornar200() throws Exception {
    when(tarjetaCreditoService.setTarjetaPrincipal(1L)).thenReturn(response);

    mockMvc
        .perform(put("/tarjetas/1/set-principal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
    verify(tarjetaCreditoService, times(1)).setTarjetaPrincipal(1L);
  }

  @Test
  void deleteTarjeta_DebeRetornar204() throws Exception {
    doNothing().when(tarjetaCreditoService).deleteTarjetaCredito(1L);
    mockMvc.perform(delete("/tarjetas/1")).andExpect(status().isNoContent());
    verify(tarjetaCreditoService, times(1)).deleteTarjetaCredito(1L);
  }
}
