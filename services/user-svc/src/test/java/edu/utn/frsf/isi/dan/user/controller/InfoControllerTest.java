package edu.utn.frsf.isi.dan.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InfoControllerTest {

  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(new InfoController("user-svc", "contenedor-abc")).build();

  @Test
  void info_devuelveServicioInstanciaIpYHora() throws Exception {
    mockMvc
        .perform(get("/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.servicio").value("user-svc"))
        .andExpect(jsonPath("$.instancia").value("contenedor-abc"))
        .andExpect(jsonPath("$.ip").isNotEmpty())
        .andExpect(jsonPath("$.hora").isNotEmpty());
  }
}
