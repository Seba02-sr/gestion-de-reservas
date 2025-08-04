package edu.utn.frsf.isi.dan.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.utn.frsf.isi.dan.user.dto.BancoRequest;
import edu.utn.frsf.isi.dan.user.dto.BancoResponse;
import edu.utn.frsf.isi.dan.user.service.BancoService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitarios para BancoController.
 * 
 * Estos tests verifican que:
 * 1- Los endpoints respondan correctamente a las peticiones HTTP.
 * 2- Se validen correctamente los parametros de entrada.
 * 3- Se manejen adecuadamente los errores y excepciones.
 * 4- Se devuelvan las respuestas esperadas.
 */

@WebMvcTest(BancoController.class)
@DisplayName("Tests del COntrolador de Bancos")
public class BancoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BancoService bancoService;

    @Autowired
    private ObjectMapper objectMapper;

    private BancoRequest bancoRequest;
    private BancoResponse bancoResponse;
    private List<BancoResponse> bancosResponse;

    @BeforeEach
    void setUp() {
        // Inicializar los objetos necesarios para los tests
        bancoRequest = new BancoRequest("Banco de Prueba");
        bancoResponse = BancoResponse.builder()
                .id(1)
                .nombre("Banco de Prueba")
                .activo(true)
                .fechaRegistro(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .fechaEliminado(null)
                .fechaModificado(null)
                .build();
        bancosResponse = List.of(bancoResponse);

        // Configurar el comportamiento del servicio mockeado
        Mockito.when(bancoService.createBanco(Mockito.any(BancoRequest.class))).thenReturn(bancoResponse);
        Mockito.when(bancoService.getAllBancos()).thenReturn(bancosResponse);
    }

    @Test
    @DisplayName("Crear un banco exitosamente - debe retornar 201 y el objeto creado")
    void createBanco_DebeRetornar201_CuandoBancoEsValido() throws Exception {
        mockMvc.perform(post("/bancos")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(bancoRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(bancoResponse.id()))
                .andExpect(jsonPath("$.nombre").value(bancoResponse.nombre()))
                .andExpect(jsonPath("$.activo").value(bancoResponse.activo()))
                .andExpect(jsonPath("$.fechaRegistro").value(bancoResponse.fechaRegistro().toString()));

        verify(bancoService, times(1)).createBanco(any(BancoRequest.class));
    }

    @Test
    @DisplayName("Obtener todos los bancos exitosamente - debe retornar 200 y una lista de bancos")
    void getAllBancos_DebeRetornar200_CuandoHayBancos() throws Exception {
        mockMvc.perform(get("/bancos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(bancosResponse.size()))
                .andExpect(jsonPath("$[0].id").value(bancoResponse.id()))
                .andExpect(jsonPath("$[0].nombre").value(bancoResponse.nombre()))
                .andExpect(jsonPath("$[0].activo").value(bancoResponse.activo()))
                .andExpect(jsonPath("$[0].fechaRegistro").value(bancoResponse.fechaRegistro().toString()));

        verify(bancoService, times(1)).getAllBancos();
    }

    @Test
    @DisplayName("Obtener banco por ID exitosamente - debe retornar 200 y el banco")
    void getBancoById_DebeRetornar200() throws Exception {
        when(bancoService.getBancoById(1)).thenReturn(bancoResponse);

        mockMvc.perform(get("/bancos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bancoResponse.id()))
                .andExpect(jsonPath("$.nombre").value(bancoResponse.nombre()));

        verify(bancoService, times(1)).getBancoById(1);
    }

    @Test
    @DisplayName("Obtener banco con ID inválido - debe retornar 400")
    void getBancoById_DebeRetornar400_CuandoIdNoExiste() throws Exception {

        given(bancoService.getBancoById(-5)).willThrow(new EntityNotFoundException("Banco no encontrado"));

        mockMvc.perform(get("/bancos/-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Actualizar banco exitosamente - debe retornar 200 y el banco actualizado")
    void updateBanco_DebeRetornar200() throws Exception {
        BancoResponse actualizado = BancoResponse.builder()
                .id(1)
                .nombre("Banco Actualizado")
                .activo(true)
                .fechaRegistro(bancoResponse.fechaRegistro())
                .fechaModificado(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();

        when(bancoService.updateBanco(eq(1), any(BancoRequest.class))).thenReturn(actualizado);

        mockMvc.perform(put("/bancos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bancoRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Banco Actualizado"))
                .andExpect(jsonPath("$.fechaModificado").value(actualizado.fechaModificado().toString()));

        verify(bancoService, times(1)).updateBanco(eq(1), any(BancoRequest.class));
    }

    @Test
    @DisplayName("Actualizar banco con ID inválido - debe retornar 400")
    void updateBanco_DebeRetornar400_CuandoIdEsInvalido() throws Exception {
        mockMvc.perform(put("/bancos/-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bancoRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Eliminar banco exitosamente - debe retornar 200 y el banco eliminado")
    void deleteBanco_DebeRetornar200() throws Exception {
        BancoResponse eliminado = BancoResponse.builder()
                .id(1)
                .nombre("Banco Eliminado")
                .activo(false)
                .fechaRegistro(bancoResponse.fechaRegistro())
                .fechaEliminado(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
                .build();

        when(bancoService.deleteBanco(1)).thenReturn(eliminado);

        mockMvc.perform(delete("/bancos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.fechaEliminado").value(eliminado.fechaEliminado().toString()));

        verify(bancoService, times(1)).deleteBanco(1);
    }

    @Test
    @DisplayName("Eliminar banco con ID inválido - debe retornar 400")
    void deleteBanco_DebeRetornar400_CuandoIdEsInvalido() throws Exception {
        mockMvc.perform(delete("/bancos/-3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Buscar bancos por nombre exitosamente - debe retornar 200 y una lista")
    void getBancosByNombre_DebeRetornar200() throws Exception {
        when(bancoService.getBancosByNombre("Prueba")).thenReturn(bancosResponse);

        mockMvc.perform(get("/bancos/buscar").param("nombre", "Prueba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Banco de Prueba"));

        verify(bancoService, times(1)).getBancosByNombre("Prueba");
    }

    @Test
    @DisplayName("Buscar bancos con nombre vacío - debe retornar 400")
    void getBancosByNombre_DebeRetornar400_CuandoNombreEsVacio() throws Exception {
        mockMvc.perform(get("/bancos/buscar").param("nombre", ""))
                .andExpect(status().isBadRequest());
    }

}
