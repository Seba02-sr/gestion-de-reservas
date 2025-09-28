package edu.utn.frsf.isi.dan.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.mapper.UsuarioMapper;
import edu.utn.frsf.isi.dan.user.model.Usuario;
import edu.utn.frsf.isi.dan.user.service.UserService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@DisplayName("Tests del Controlador de Usuarios")
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserService userService;
  @MockitoBean private UsuarioMapper usuarioMapper;
  @Autowired private ObjectMapper objectMapper;

  private HuespedRequest huespedRequest;
  private PropietarioRequest propietarioRequest;

  @BeforeEach
  void setUp() {
    huespedRequest =
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
                        .nombreTitular("Juan")
                        .fechaVencimiento("12/25")
                        .cvc("123")
                        .esPrincipal(true)
                        .idBanco(1)
                        .build()))
            .build();

    propietarioRequest =
        PropietarioRequest.builder()
            .nombre("Prop")
            .email("p@example.com")
            .telefono("321")
            .dni("2222")
            .username("prop")
            .password("secret")
            .idHotel(1L)
            .cuentaBancaria(
                edu.utn.frsf.isi.dan.user.dto.CuentaRequest.builder()
                    .numeroCuenta("123")
                    .idBanco(1)
                    .build())
            .build();
  }

  @Test
  void crearHuesped_DebeRetornar201() throws Exception {
    when(userService.crearUsuarioHuesped(any()))
        .thenReturn(new edu.utn.frsf.isi.dan.user.model.Huesped());
    mockMvc
        .perform(
            post("/users/huesped")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(huespedRequest)))
        .andExpect(status().isCreated());
    verify(userService, times(1)).crearUsuarioHuesped(any());
  }

  @Test
  void crearPropietario_DebeRetornar201() throws Exception {
    when(userService.crearUsuarioPropietario(any()))
        .thenReturn(new edu.utn.frsf.isi.dan.user.model.Propietario());
    mockMvc
        .perform(
            post("/users/propietario")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(propietarioRequest)))
        .andExpect(status().isCreated());
    verify(userService, times(1)).crearUsuarioPropietario(any());
  }

  @Test
  void buscarPorNombre_DebeRetornar200() throws Exception {
    Page<Usuario> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(userService.buscarPorNombre(eq("Juan"), any())).thenReturn(page);
    mockMvc.perform(get("/users").param("nombre", "Juan")).andExpect(status().isOk());
    verify(userService, times(1)).buscarPorNombre(eq("Juan"), any());
  }

  @Test
  void buscarPorDniExacto_DebeRetornar404_CuandoNoExiste() throws Exception {
    when(userService.buscarPorDniExacto("999")).thenReturn(null);
    mockMvc.perform(get("/users/dni/999")).andExpect(status().isNotFound());
    verify(userService, times(1)).buscarPorDniExacto("999");
  }

  @Test
  void buscarPorDni_DebeRetornar200() throws Exception {
    Page<Usuario> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
    when(userService.buscarPorDni(eq("11"), any())).thenReturn(page);
    mockMvc.perform(get("/users/buscar-dni").param("dni", "11")).andExpect(status().isOk());
    verify(userService, times(1)).buscarPorDni(eq("11"), any());
  }
}
