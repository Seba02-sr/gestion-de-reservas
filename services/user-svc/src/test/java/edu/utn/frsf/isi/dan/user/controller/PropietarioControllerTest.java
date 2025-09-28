package edu.utn.frsf.isi.dan.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.user.dto.CuentaRequest;
import edu.utn.frsf.isi.dan.user.dto.CuentaResponse;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioResponse;
import edu.utn.frsf.isi.dan.user.service.PropietarioService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PropietarioController.class)
@DisplayName("Tests del Controlador de Propietarios")
class PropietarioControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private PropietarioService propietarioService;
  @Autowired private ObjectMapper objectMapper;

  private PropietarioRequest request;
  private PropietarioResponse response;

  @BeforeEach
  void setUp() {
    request =
        PropietarioRequest.builder()
            .nombre("Propietario")
            .email("p@example.com")
            .telefono("123")
            .dni("2222")
            .username("prop")
            .password("secret")
            .idHotel(1L)
            .cuentaBancaria(
                CuentaRequest.builder()
                    .numeroCuenta("0001")
                    .cbu("cbu")
                    .alias("alias")
                    .idBanco(1)
                    .build())
            .build();

    response =
        PropietarioResponse.builder()
            .id(1)
            .nombre("Propietario")
            .dni("2222")
            .idHotel(1L)
            .cuentaBancaria(CuentaResponse.builder().id(10).numeroCuenta("0001").idBanco(1).build())
            .build();
  }

  @Test
  void listar_DebeRetornar200() throws Exception {
    when(propietarioService.getAllPropietarios()).thenReturn(List.of(response));
    mockMvc.perform(get("/propietarios")).andExpect(status().isOk());
    verify(propietarioService, times(1)).getAllPropietarios();
  }

  @Test
  void obtenerPorId_DebeRetornar200() throws Exception {
    when(propietarioService.getPropietarioById(1L)).thenReturn(response);
    mockMvc
        .perform(get("/propietarios/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
    verify(propietarioService, times(1)).getPropietarioById(1L);
  }

  @Test
  void actualizar_DebeRetornar200() throws Exception {
    when(propietarioService.actualizarPropietario(eq(1L), any(PropietarioRequest.class)))
        .thenReturn(response);
    mockMvc
        .perform(
            put("/propietarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
    verify(propietarioService, times(1))
        .actualizarPropietario(eq(1L), any(PropietarioRequest.class));
  }

  @Test
  void eliminar_DebeRetornar405() throws Exception {
    mockMvc.perform(delete("/propietarios/1")).andExpect(status().isMethodNotAllowed());
    verifyNoInteractions(propietarioService);
  }

  @Test
  void buscarPorNombre_DebeRetornar200() throws Exception {
    when(propietarioService.buscarPorNombre("Pro")).thenReturn(List.of(response));
    mockMvc.perform(get("/propietarios/buscar").param("nombre", "Pro")).andExpect(status().isOk());
    verify(propietarioService, times(1)).buscarPorNombre("Pro");
  }

  @Test
  void buscarPorDniExacto_DebeRetornar200() throws Exception {
    when(propietarioService.buscarPorDniExacto("2222")).thenReturn(response);
    mockMvc.perform(get("/propietarios/dni/2222")).andExpect(status().isOk());
    verify(propietarioService, times(1)).buscarPorDniExacto("2222");
  }
}
