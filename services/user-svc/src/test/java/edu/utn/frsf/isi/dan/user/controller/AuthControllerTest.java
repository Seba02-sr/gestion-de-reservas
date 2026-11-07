package edu.utn.frsf.isi.dan.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.utn.frsf.isi.dan.user.dto.GoogleAuthResponse;
import edu.utn.frsf.isi.dan.user.exception.CredencialesInvalidasException;
import edu.utn.frsf.isi.dan.user.exception.GoogleNoConfiguradoException;
import edu.utn.frsf.isi.dan.user.service.AuthService;
import edu.utn.frsf.isi.dan.user.service.GoogleAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AuthService authService;
  @MockitoBean private GoogleAuthService googleAuthService;

  private static final String OK = "{\"username\":\"juan\",\"password\":\"secret\"}";

  @Test
  void login_ok_devuelveLoMismoQueGoogle() throws Exception {
    when(authService.login(any()))
        .thenReturn(new GoogleAuthResponse(5, "juan@x.com", "Juan Perez", "Juan", "Perez"));
    mockMvc
        .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(OK))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(5))
        .andExpect(jsonPath("$.email").value("juan@x.com"))
        .andExpect(jsonPath("$.nombre").value("Juan Perez"));
  }

  @Test
  void login_credencialesInvalidas_401() throws Exception {
    when(authService.login(any())).thenThrow(new CredencialesInvalidasException());
    mockMvc
        .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(OK))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void login_cuerpoVacio_400() throws Exception {
    mockMvc
        .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void google_sinConfigurar_503() throws Exception {
    when(googleAuthService.authenticate(any())).thenThrow(new GoogleNoConfiguradoException());
    mockMvc
        .perform(
            post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"abc\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void google_tokenInvalido_401() throws Exception {
    when(googleAuthService.authenticate(any())).thenThrow(new SecurityException("token inválido"));
    mockMvc
        .perform(
            post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"abc\"}"))
        .andExpect(status().isUnauthorized());
  }
}
