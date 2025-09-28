package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.user.dao.HuespedRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedRequest;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.dto.TarjetaCreditoRequest;
import edu.utn.frsf.isi.dan.user.mapper.HuespedMapper;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HuespedServiceTest {

  @Mock private HuespedRepository huespedRepository;
  @Mock private HuespedMapper huespedMapper;

  @InjectMocks private HuespedService huespedService;

  private Huesped entidad;
  private HuespedResponse response;
  private HuespedRequest request;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    TarjetaCredito tarjeta = TarjetaCredito.builder().id(1).esPrincipal(true).build();
    entidad = Huesped.builder().tarjetaCredito(new ArrayList<>(List.of(tarjeta))).build();

    request =
        HuespedRequest.builder()
            .nombre("Juan")
            .email("j@e.com")
            .telefono("123")
            .dni("1111")
            .username("juan")
            .password("pwd")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .tarjetasCredito(
                List.of(
                    TarjetaCreditoRequest.builder()
                        .numero("4")
                        .nombreTitular("Juan")
                        .cvc("1")
                        .fechaVencimiento("12/25")
                        .esPrincipal(true)
                        .idBanco(1)
                        .build()))
            .build();

    response = HuespedResponse.builder().id(1).nombre("Juan").dni("1111").build();
  }

  @Test
  void getAll_DeberiaDevolverListaDTO() {
    when(huespedRepository.findByActivoTrue()).thenReturn(List.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);

    List<HuespedResponse> out = huespedService.getAllHuespedes();
    assertEquals(1, out.size());
  }

  @Test
  void getById_DeberiaRetornarDTO_CuandoExiste() {
    when(huespedRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);
    HuespedResponse out = huespedService.getHuespedById(1L);
    assertEquals(1, out.id());
  }

  @Test
  void getById_DeberiaLanzar404_CuandoNoExiste() {
    when(huespedRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> huespedService.getHuespedById(1L));
  }

  @Test
  void actualizar_DeberiaGuardarYRetornarDTO() {
    when(huespedRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(entidad));
    doAnswer(
            inv -> {
              // marcar que el mapper aplicó cambios
              HuespedRequest req = inv.getArgument(0);
              Huesped e = inv.getArgument(1);
              e.setNombre(req.nombre());
              return null;
            })
        .when(huespedMapper)
        .updateEntityFromRequest(eq(request), any(Huesped.class));
    when(huespedRepository.save(entidad)).thenReturn(entidad);
    when(huespedMapper.toResponse(entidad)).thenReturn(response);

    HuespedResponse out = huespedService.actualizarHuesped(1L, request);
    assertEquals("Juan", out.nombre());
    // inversa tarjeta -> huesped seteada
    assertEquals(entidad, entidad.getTarjetaCredito().get(0).getHuesped());
  }

  @Test
  void eliminar_DeberiaHacerSoftDelete() {
    when(huespedRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(entidad));
    when(huespedRepository.save(any(Huesped.class))).thenAnswer(inv -> inv.getArgument(0));
    assertDoesNotThrow(() -> huespedService.eliminarHuesped(1L));
    assertFalse(entidad.getActivo());
    verify(huespedRepository, times(1)).save(entidad);
  }

  @Test
  void buscarPorNombre_DeberiaRetornarListaDTO() {
    when(huespedRepository.findByActivoTrueAndNombreContainingIgnoreCase("Ju"))
        .thenReturn(List.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);
    List<HuespedResponse> out = huespedService.buscarPorNombre("Ju");
    assertEquals(1, out.size());
  }

  @Test
  void buscarPorDni_DeberiaRetornarDTO() {
    when(huespedRepository.findByActivoTrueAndDni("1111")).thenReturn(Optional.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);
    HuespedResponse out = huespedService.buscarPorDni("1111");
    assertEquals("Juan", out.nombre());
  }
}
