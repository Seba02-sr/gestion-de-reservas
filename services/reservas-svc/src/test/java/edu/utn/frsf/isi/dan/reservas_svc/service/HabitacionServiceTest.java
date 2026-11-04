package edu.utn.frsf.isi.dan.reservas_svc.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import edu.utn.frsf.isi.dan.reservas_svc.repository.HabitacionRepository;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

@ExtendWith(MockitoExtension.class)
class HabitacionServiceTest {

  @Mock private HabitacionRepository habitacionRepository;
  @Mock private MongoTemplate mongoTemplate;
  @InjectMocks private HabitacionService habitacionService;

  @Test
  void handleEvent_cerrarHotel_noLanzaExcepcionNiTocaLaBase() {
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.CERRAR_HOTEL)
            .hotelId(1)
            .habitacionIds(List.of(10L, 11L))
            .fechaCierre(LocalDate.of(2026, 11, 4))
            .build();

    assertDoesNotThrow(() -> habitacionService.handleEvent(evento));
    verifyNoInteractions(habitacionRepository, mongoTemplate);
  }
}
