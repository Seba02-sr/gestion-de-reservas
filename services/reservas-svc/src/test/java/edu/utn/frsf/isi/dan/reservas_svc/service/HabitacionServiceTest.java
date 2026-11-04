package edu.utn.frsf.isi.dan.reservas_svc.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.repository.HabitacionRepository;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.TarifaDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

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

  @Test
  void handleEvent_actualizarPrecio_actualizaTodasLasHabitacionesDelTipo() {
    when(mongoTemplate.updateMulti(
            any(Query.class), any(UpdateDefinition.class), eq(Habitacion.class)))
        .thenReturn(UpdateResult.acknowledged(3, 3L, null));
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(3).nuevoPrecio(18000.0).build())
            .build();

    habitacionService.handleEvent(evento);

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
    verify(mongoTemplate).updateMulti(query.capture(), update.capture(), eq(Habitacion.class));
    assertEquals(3, query.getValue().getQueryObject().get("idTipoHabitacion"));
    assertEquals(
        18000.0,
        ((org.bson.Document) update.getValue().getUpdateObject().get("$set")).get("precioNoche"));
  }

  @Test
  void handleEvent_actualizarPrecio_sinDatosLanzaIllegalArgument() {
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(3).build())
            .build();

    assertThrows(IllegalArgumentException.class, () -> habitacionService.handleEvent(evento));
  }
}
