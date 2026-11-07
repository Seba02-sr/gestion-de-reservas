package edu.utn.frsf.isi.dan.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.TipoHabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.TipoHabitacionMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.TipoHabitacionMapperImpl;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TipoHabitacionServiceTest {

  @Mock private TipoHabitacionRepository tipoHabitacionRepository;
  @Mock private HabitacionRepository habitacionRepository;
  @Mock private TarifaRepository tarifaRepository;
  @Spy private TipoHabitacionMapper tipoHabitacionMapper = new TipoHabitacionMapperImpl();

  @InjectMocks private TipoHabitacionService service;

  private final TipoHabitacionRequest request =
      new TipoHabitacionRequest("SUITE", "Suite con living", 4);

  @Test
  void crear_guardaYDevuelveElTipoConSuId() {
    when(tipoHabitacionRepository.save(any(TipoHabitacion.class)))
        .thenAnswer(
            inv -> {
              TipoHabitacion t = inv.getArgument(0);
              t.setId(10);
              return t;
            });

    TipoHabitacionResponse r = service.crear(request);

    assertEquals(new TipoHabitacionResponse(10, "SUITE", "Suite con living", 4), r);
  }

  @Test
  void actualizar_cambiaTodosLosCamposMenosElId() {
    TipoHabitacion existente = new TipoHabitacion(10, "VIEJO", "viejo", 1);
    when(tipoHabitacionRepository.findById(10)).thenReturn(Optional.of(existente));
    when(tipoHabitacionRepository.save(existente)).thenReturn(existente);

    TipoHabitacionResponse r = service.actualizar(10, request);

    assertEquals(new TipoHabitacionResponse(10, "SUITE", "Suite con living", 4), r);
  }

  @Test
  void actualizarObtenerYEliminar_inexistente_lanzanEntityNotFound() {
    when(tipoHabitacionRepository.findById(99)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> service.actualizar(99, request));
    assertThrows(EntityNotFoundException.class, () -> service.obtener(99));
    assertThrows(EntityNotFoundException.class, () -> service.eliminar(99));
  }

  @Test
  void listar_devuelveTodos() {
    when(tipoHabitacionRepository.findAll())
        .thenReturn(List.of(new TipoHabitacion(1, "SINGLE", "Single", 1)));

    assertEquals(1, service.listar().size());
  }

  @Test
  void eliminar_sinUso_borra() {
    TipoHabitacion tipo = new TipoHabitacion(10, "SUITE", "x", 4);
    when(tipoHabitacionRepository.findById(10)).thenReturn(Optional.of(tipo));

    service.eliminar(10);

    verify(tipoHabitacionRepository).delete(tipo);
  }

  @Test
  void eliminar_conHabitacionesOConTarifas_lanzaIllegalState() {
    TipoHabitacion tipo = new TipoHabitacion(10, "SUITE", "x", 4);
    when(tipoHabitacionRepository.findById(10)).thenReturn(Optional.of(tipo));
    when(habitacionRepository.existsByTipoHabitacionId(10)).thenReturn(true);

    assertThrows(IllegalStateException.class, () -> service.eliminar(10));

    when(habitacionRepository.existsByTipoHabitacionId(10)).thenReturn(false);
    when(tarifaRepository.existsByTipoHabitacionId(10)).thenReturn(true);
    assertThrows(IllegalStateException.class, () -> service.eliminar(10));
    verify(tipoHabitacionRepository, never()).delete(any());
  }
}
