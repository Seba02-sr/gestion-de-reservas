package edu.utn.frsf.isi.dan.gestion.messaging;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class HabitacionEventPublisherTest {

  @Mock private RabbitTemplate rabbitTemplate;

  @SuppressWarnings("unused")
  private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

  @InjectMocks private HabitacionEventPublisher publisher;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(publisher, "objectMapper", objectMapper);
    ReflectionTestUtils.setField(publisher, "exchange", "dan.exchange");
    ReflectionTestUtils.setField(publisher, "routingKeyPrefix", "dan.habitacion");
  }

  @Test
  void publicar_enviaAlExchangeConLaRoutingKeyDelTipoYElJsonDelEvento() throws Exception {
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.CREAR)
            .hotelId(1)
            .habitacion(
                HabitacionDTO.builder()
                    .habitacionId(10L)
                    .numero(101)
                    .precioNoche(15000.0)
                    .amenities(List.of("WIFI"))
                    .hotel(HotelDTO.builder().id(1).latitud(-31.6).longitud(-60.7).build())
                    .build())
            .build();

    publisher.publicar(new HabitacionEventoPublicado(evento));

    org.mockito.ArgumentCaptor<String> json = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("dan.exchange"), eq("dan.habitacion.crear"), json.capture());
    // el mensaje que recibe reservas-svc se puede deserializar al mismo evento
    assertEquals(evento, objectMapper.readValue(json.getValue(), HabitacionEvent.class));
  }

  @Test
  void publicar_usaUnaRoutingKeyPorTipoQueMatcheaElBindingDanHabitacionNumeral() {
    assertEquals("dan.habitacion.crear", publisher.routingKey(TipoEvento.CREAR));
    assertEquals("dan.habitacion.actualizar", publisher.routingKey(TipoEvento.ACTUALIZAR_DATOS));
    assertEquals("dan.habitacion.eliminar", publisher.routingKey(TipoEvento.ELIMINAR));
    assertEquals("dan.habitacion.cerrar-hotel", publisher.routingKey(TipoEvento.CERRAR_HOTEL));
    assertEquals("dan.habitacion.precio", publisher.routingKey(TipoEvento.ACTUALIZAR_PRECIO));
  }

  @Test
  void publicar_cerrarHotel_serializaFechaDeCierreEIds() throws Exception {
    HabitacionEvent evento =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.CERRAR_HOTEL)
            .hotelId(1)
            .habitacionIds(List.of(10L, 11L))
            .fechaCierre(LocalDate.of(2026, 11, 4))
            .build();

    publisher.publicar(new HabitacionEventoPublicado(evento));

    org.mockito.ArgumentCaptor<String> json = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(rabbitTemplate)
        .convertAndSend(eq("dan.exchange"), eq("dan.habitacion.cerrar-hotel"), json.capture());
    assertEquals(evento, objectMapper.readValue(json.getValue(), HabitacionEvent.class));
  }

  @Test
  void publicar_siRabbitFalla_noPropagaLaExcepcion() {
    doThrow(new RuntimeException("rabbit caído"))
        .when(rabbitTemplate)
        .convertAndSend(anyString(), anyString(), any(String.class));

    assertDoesNotThrow(
        () ->
            publisher.publicar(
                new HabitacionEventoPublicado(
                    HabitacionEvent.builder().tipoEvento(TipoEvento.ELIMINAR).build())));
  }

  @Test
  void publicar_seEjecutaSoloDespuesDelCommit() throws Exception {
    Method metodo =
        HabitacionEventPublisher.class.getMethod("publicar", HabitacionEventoPublicado.class);

    TransactionalEventListener anotacion = metodo.getAnnotation(TransactionalEventListener.class);

    assertNotNull(anotacion);
    assertEquals(TransactionPhase.AFTER_COMMIT, anotacion.phase());
    assertFalse(anotacion.fallbackExecution());
  }
}
