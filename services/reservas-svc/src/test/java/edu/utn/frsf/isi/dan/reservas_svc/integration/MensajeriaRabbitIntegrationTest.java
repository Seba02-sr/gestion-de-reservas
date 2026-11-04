package edu.utn.frsf.isi.dan.reservas_svc.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.service.HabitacionService;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import edu.utn.frsf.isi.dan.shared.TarifaDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Contrato de mensajería con un RabbitMQ y un MongoDB reales (contenedores aislados): publica los
 * eventos EXACTAMENTE como lo hace gestion-svc (un {@code String} JSON enviado con un {@link
 * RabbitTemplate} con {@link Jackson2JsonMessageConverter}, al exchange {@code dan.exchange} con la
 * routing key {@code dan.habitacion.<accion>}) y comprueba que el listener de reservas-svc los
 * consume y actualiza la réplica. Se saltea si no hay Docker.
 */
@SpringBootTest(properties = {"reservas.crear-indices=true"})
class MensajeriaRabbitIntegrationTest {

  private static GenericContainer<?> mongo;
  private static GenericContainer<?> rabbit;

  @BeforeAll
  static void arrancar() {
    assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(),
        "Docker no está disponible: se saltean los tests de integración con RabbitMQ");
    mongo =
        new GenericContainer<>("mongo:7")
            .withExposedPorts(27017)
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1));
    rabbit =
        new GenericContainer<>("rabbitmq:3-management")
            .withExposedPorts(5672)
            .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));
    mongo.start();
    rabbit.start();
  }

  @AfterAll
  static void detener() {
    if (rabbit != null) {
      rabbit.stop();
    }
    if (mongo != null) {
      mongo.stop();
    }
  }

  @DynamicPropertySource
  static void propiedades(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.data.mongodb.uri",
        () -> "mongodb://" + mongo.getHost() + ":" + mongo.getMappedPort(27017) + "/reservas_mq");
    registry.add("spring.rabbitmq.host", () -> rabbit.getHost());
    registry.add("spring.rabbitmq.port", () -> rabbit.getMappedPort(5672));
    registry.add("spring.rabbitmq.username", () -> "guest");
    registry.add("spring.rabbitmq.password", () -> "guest");
  }

  @Autowired private ConnectionFactory connectionFactory;
  @Autowired private RabbitAdmin rabbitAdmin;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private HabitacionService habitacionService;
  @Autowired private MongoTemplate mongoTemplate;

  /** Publica como gestion-svc: String JSON + RabbitTemplate con Jackson2JsonMessageConverter. */
  private void publicarComoGestion(String accion, HabitacionEvent evento) throws Exception {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    template.convertAndSend(
        "dan.exchange", "dan.habitacion." + accion, objectMapper.writeValueAsString(evento));
  }

  private Habitacion esperarHabitacion(long id) {
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(200))
        .until(() -> habitacionService.findByHabitacionId(id).isPresent());
    return habitacionService.findByHabitacionId(id).orElseThrow();
  }

  @Test
  void loQuePublicaGestionSvcLoConsumeReservasSvc_crearPrecioYCerrarHotel() throws Exception {
    // el listener declara exchange, cola y binding al arrancar su contenedor
    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> rabbitAdmin.getQueueProperties("habitacion.topic") != null);
    mongoTemplate.remove(new Query(), Habitacion.class);

    HotelDTO hotel =
        HotelDTO.builder()
            .id(1)
            .nombre("Hotel Castelar")
            .domicilio("Bv. Pellegrini 2500")
            .latitud(-31.6333)
            .longitud(-60.7)
            .categoria(4)
            .build();
    HabitacionDTO dto =
        HabitacionDTO.builder()
            .habitacionId(77L)
            .numero(101)
            .piso(1)
            .disponible(true)
            .capacidad(2)
            .precioNoche(null)
            .tipoHabitacionId(3)
            .tipoHabitacion("DOBLE")
            .amenities(List.of("WIFI", "PILETA"))
            .hotel(hotel)
            .build();

    // 1) CREAR (alta de habitación en gestion-svc), enviado dos veces: no se duplica
    HabitacionEvent crear =
        HabitacionEvent.builder().tipoEvento(TipoEvento.CREAR).hotelId(1).habitacion(dto).build();
    publicarComoGestion("crear", crear);
    publicarComoGestion("crear", crear);
    Habitacion h = esperarHabitacion(77L);
    assertThat(h.getNumero()).isEqualTo(101);
    assertThat(h.getHotel().getUbicacion().getX()).isEqualTo(-60.7); // longitud
    assertThat(h.getHotel().getUbicacion().getY()).isEqualTo(-31.6333); // latitud
    assertThat(h.getAmenities()).containsExactly("WIFI", "PILETA");

    // 2) ACTUALIZAR_PRECIO (cambió la tarifa vigente del tipo 3)
    publicarComoGestion(
        "precio",
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.ACTUALIZAR_PRECIO)
            .tarifa(TarifaDTO.builder().tipoHabitacionId(3).nuevoPrecio(18000.0).build())
            .build());
    await()
        .atMost(Duration.ofSeconds(30))
        .until(
            () ->
                Double.valueOf(18000.0)
                    .equals(
                        habitacionService.findByHabitacionId(77L).orElseThrow().getPrecioNoche()));

    // 3) CERRAR_HOTEL (con la fecha como la serializa gestion-svc: ISO yyyy-MM-dd)
    HabitacionEvent cerrar =
        HabitacionEvent.builder()
            .tipoEvento(TipoEvento.CERRAR_HOTEL)
            .hotelId(1)
            .habitacionIds(List.of(77L))
            .fechaCierre(LocalDate.of(2026, 11, 4))
            .build();
    publicarComoGestion("cerrar-hotel", cerrar);
    publicarComoGestion("cerrar-hotel", cerrar);
    await()
        .atMost(Duration.ofSeconds(30))
        .until(
            () ->
                Boolean.FALSE.equals(
                    habitacionService.findByHabitacionId(77L).orElseThrow().getDisponible()));

    Habitacion cerrada = habitacionService.findByHabitacionId(77L).orElseThrow();
    assertThat(cerrada.getReservas()).hasSize(1);
    assertThat(cerrada.getReservas().get(0).getEstadoReserva()).isEqualTo(EstadoReserva.CERRADA);
    assertThat(cerrada.getReservas().get(0).getCheckOut()).isNull();
    assertThat(mongoTemplate.count(new Query(), Habitacion.class)).isEqualTo(1);
  }
}
