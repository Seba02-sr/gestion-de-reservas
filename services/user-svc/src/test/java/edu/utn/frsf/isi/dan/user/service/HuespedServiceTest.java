package edu.utn.frsf.isi.dan.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.user.dao.HuespedRepository;
import edu.utn.frsf.isi.dan.user.dto.HuespedResponse;
import edu.utn.frsf.isi.dan.user.dto.HuespedUpdateRequest;
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
  private HuespedUpdateRequest request;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    TarjetaCredito tarjeta = TarjetaCredito.builder().id(1).esPrincipal(true).build();
    entidad = Huesped.builder().tarjetaCredito(new ArrayList<>(List.of(tarjeta))).build();

    request =
        HuespedUpdateRequest.builder()
            .nombre("Juan")
            .email("j@e.com")
            .telefono("123")
            .dni("1111")
            .fechaNacimiento(LocalDate.of(1990, 1, 1))
            .build();

    response = HuespedResponse.builder().id(1).nombre("Juan").dni("1111").build();
  }

  @Test
  void getAll_DeberiaDevolverListaDTO() {
    when(huespedRepository.findAll()).thenReturn(List.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);

    List<HuespedResponse> out = huespedService.getAllHuespedes();
    assertEquals(1, out.size());
  }

  @Test
  void getById_DeberiaRetornarDTO_CuandoExiste() {
    when(huespedRepository.findById(1)).thenReturn(Optional.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);
    HuespedResponse out = huespedService.getHuespedById(1);
    assertEquals(1, out.id());
  }

  @Test
  void getById_DeberiaLanzar404_CuandoNoExiste() {
    when(huespedRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> huespedService.getHuespedById(1));
  }

  @Test
  void actualizar_DeberiaGuardarYRetornarDTO() {
    when(huespedRepository.findById(1)).thenReturn(Optional.of(entidad));
    doAnswer(
            inv -> {
              // marcar que el mapper aplicó cambios
              HuespedUpdateRequest req = inv.getArgument(0);
              Huesped e = inv.getArgument(1);
              e.setNombre(req.nombre());
              return null;
            })
        .when(huespedMapper)
        .updateEntityFromRequest(eq(request), any(Huesped.class));
    when(huespedRepository.save(entidad)).thenReturn(entidad);
    when(huespedMapper.toResponse(entidad)).thenReturn(response);

    HuespedResponse out = huespedService.actualizarHuesped(1, request);
    assertEquals("Juan", out.nombre());
    assertNotNull(entidad.getFechaModificado());
    // las tarjetas no se tocan en un update
    assertEquals(1, entidad.getTarjetaCredito().size());
  }

  @Test
  void eliminar_DeberiaBorrarFisicamente() {
    when(huespedRepository.findById(1)).thenReturn(Optional.of(entidad));
    assertDoesNotThrow(() -> huespedService.eliminarHuesped(1));
    verify(huespedRepository, times(1)).delete(entidad);
    verify(huespedRepository, never()).save(any(Huesped.class));
    assertTrue(entidad.getActivo());
  }

  @Test
  void eliminar_DeberiaLanzar404_CuandoNoExiste() {
    when(huespedRepository.findById(1)).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> huespedService.eliminarHuesped(1));
    verify(huespedRepository, never()).delete(any(Huesped.class));
  }

  @Test
  void buscarPorNombre_DeberiaRetornarListaDTO() {
    when(huespedRepository.findByNombreContainingIgnoreCase("Ju")).thenReturn(List.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);
    List<HuespedResponse> out = huespedService.buscarPorNombre("Ju");
    assertEquals(1, out.size());
  }

  @Test
  void buscarPorDni_DeberiaRetornarDTO() {
    when(huespedRepository.findByDni("1111")).thenReturn(Optional.of(entidad));
    when(huespedMapper.toResponse(entidad)).thenReturn(response);
    HuespedResponse out = huespedService.buscarPorDni("1111");
    assertEquals("Juan", out.nombre());
  }
}
