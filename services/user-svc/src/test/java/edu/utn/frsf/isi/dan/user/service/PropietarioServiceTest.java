package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.user.dao.PropietarioRepository;
import edu.utn.frsf.isi.dan.user.dto.CuentaRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioRequest;
import edu.utn.frsf.isi.dan.user.dto.PropietarioResponse;
import edu.utn.frsf.isi.dan.user.mapper.PropietarioMapper;
import edu.utn.frsf.isi.dan.user.model.Propietario;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PropietarioServiceTest {

  @Mock private PropietarioRepository propietarioRepository;
  @Mock private PropietarioMapper propietarioMapper;

  @InjectMocks private PropietarioService propietarioService;

  private Propietario entidad;
  private PropietarioResponse response;
  private PropietarioRequest request;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    entidad = Propietario.builder().idHotel(1L).build();
    response = PropietarioResponse.builder().id(1).idHotel(1L).dni("2222").build();
    request =
        PropietarioRequest.builder()
            .nombre("Prop")
            .email("p@e.com")
            .telefono("123")
            .dni("2222")
            .username("prop")
            .password("pwd")
            .idHotel(1L)
            .cuentaBancaria(
                CuentaRequest.builder()
                    .numeroCuenta("0001")
                    .cbu("cbu")
                    .alias("a")
                    .idBanco(1)
                    .build())
            .build();
  }

  @Test
  void getAll_DeberiaRetornarListaDTO() {
    when(propietarioRepository.findByActivoTrue()).thenReturn(List.of(entidad));
    when(propietarioMapper.toResponse(entidad)).thenReturn(response);
    List<PropietarioResponse> out = propietarioService.getAllPropietarios();
    assertEquals(1, out.size());
  }

  @Test
  void getById_DeberiaRetornarDTO() {
    when(propietarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(entidad));
    when(propietarioMapper.toResponse(entidad)).thenReturn(response);
    PropietarioResponse out = propietarioService.getPropietarioById(1L);
    assertEquals(1L, out.idHotel());
  }

  @Test
  void getById_DeberiaLanzar404_CuandoNoExiste() {
    when(propietarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> propietarioService.getPropietarioById(1L));
  }

  @Test
  void actualizar_DeberiaGuardarYRetornarDTO() {
    when(propietarioRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(entidad));
    doAnswer(
            invocation -> {
              PropietarioRequest req = invocation.getArgument(0);
              Propietario e = invocation.getArgument(1);
              e.setNombre(req.nombre());
              return null;
            })
        .when(propietarioMapper)
        .updateEntityFromRequest(eq(request), any(Propietario.class));
    when(propietarioRepository.save(entidad)).thenReturn(entidad);
    when(propietarioMapper.toResponse(entidad)).thenReturn(response);

    PropietarioResponse out = propietarioService.actualizarPropietario(1L, request);
    assertEquals("2222", out.dni());
  }

  @Test
  void buscarPorNombre_DeberiaRetornarListaDTO() {
    when(propietarioRepository.findByActivoTrueAndNombreContainingIgnoreCase("Pro"))
        .thenReturn(List.of(entidad));
    when(propietarioMapper.toResponse(entidad)).thenReturn(response);
    List<PropietarioResponse> out = propietarioService.buscarPorNombre("Pro");
    assertEquals(1, out.size());
  }

  @Test
  void buscarPorDniParcial_DeberiaRetornarListaDTO() {
    when(propietarioRepository.findByActivoTrueAndDniStartingWith("22"))
        .thenReturn(List.of(entidad));
    when(propietarioMapper.toResponse(entidad)).thenReturn(response);
    List<PropietarioResponse> out = propietarioService.buscarPorDniParcial("22");
    assertEquals(1, out.size());
  }

  @Test
  void buscarPorDniExacto_DeberiaRetornarDTO() {
    when(propietarioRepository.findByActivoTrueAndDni("2222")).thenReturn(Optional.of(entidad));
    when(propietarioMapper.toResponse(entidad)).thenReturn(response);
    PropietarioResponse out = propietarioService.buscarPorDniExacto("2222");
    assertEquals("2222", out.dni());
  }
}
