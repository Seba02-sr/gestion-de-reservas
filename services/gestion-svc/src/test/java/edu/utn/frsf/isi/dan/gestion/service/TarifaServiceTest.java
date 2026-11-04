package edu.utn.frsf.isi.dan.gestion.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import edu.utn.frsf.isi.dan.gestion.dao.TarifaRepository;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarifaServiceTest {

  @Mock private TarifaRepository tarifaRepository;
  @InjectMocks private TarifaService tarifaService;

  @Test
  void obtenerTarifaVigente_devuelveLaPrimeraDeLasVigentesDeHoy() {
    Tarifa nueva = Tarifa.builder().id(2).precioNoche(18000.0).build();
    Tarifa vieja = Tarifa.builder().id(1).precioNoche(15000.0).build();
    when(tarifaRepository.findVigentes(eq(3), eq(LocalDate.now())))
        .thenReturn(List.of(nueva, vieja));

    assertEquals(Optional.of(nueva), tarifaService.obtenerTarifaVigente(3));
  }

  @Test
  void obtenerTarifaVigente_sinTarifasVigentes_devuelveVacio() {
    when(tarifaRepository.findVigentes(eq(3), any(LocalDate.class))).thenReturn(List.of());

    assertTrue(tarifaService.obtenerTarifaVigente(3).isEmpty());
  }
}
