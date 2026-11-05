package edu.utn.frsf.isi.dan.reservas_svc.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ServicioNoDisponibleException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * El client Feign real contra un user-svc simulado con WireMock (sin Eureka ni red): éxito, 404,
 * error 500, conexión cortada, timeout y servicio caído (puerto cerrado). Cubre también la
 * traducción a 404 / 503 que hace {@link ServiciosRemotos}.
 */
@SpringBootTest(
    properties = {
      "reservas.crear-indices=false",
      "spring.rabbitmq.listener.simple.auto-startup=false",
      "spring.cloud.openfeign.client.config.default.connect-timeout=500",
      "spring.cloud.openfeign.client.config.default.read-timeout=500",
      // en esta clase el circuito no debe abrirse (se prueba aparte)
      "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=1000"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceClientIntegrationTest {

  private static final WireMockServer WIREMOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());

  static {
    WIREMOCK.start();
  }

  @DynamicPropertySource
  static void urls(DynamicPropertyRegistry registry) {
    registry.add("clients.user-svc.url", WIREMOCK::baseUrl);
    registry.add("clients.gestion-svc.url", WIREMOCK::baseUrl);
  }

  @AfterAll
  static void detener() {
    WIREMOCK.stop();
  }

  @Autowired private UserServiceClient client;
  @Autowired private ServiciosRemotos remotos;

  @BeforeEach
  void limpiar() {
    WIREMOCK.resetAll();
  }

  /** Respuesta real de user-svc: trae muchos más campos que los que reservas usa. */
  private static final String HUESPED_JSON =
      """
      {"id":7,"nombre":"Carla Gómez","email":"carla@example.com","telefono":"+54 9 342-555-1234",
       "dni":"12345678","username":"carla","activo":true,"fechaNacimiento":"1990-01-01",
       "tarjetaCredito":[{"id":1,"numero":"**** **** **** 0004","esPrincipal":true}]}
      """;

  @Test
  @Order(1)
  void exito_mapeaSoloLosCamposNecesarios_eIgnoraElResto() {
    WIREMOCK.stubFor(get(urlEqualTo("/huespedes/7")).willReturn(okJson(HUESPED_JSON)));

    HuespedInfo huesped = client.getHuesped(7);

    assertEquals(new HuespedInfo(7, "Carla Gómez", "carla@example.com"), huesped);
    WIREMOCK.verify(1, getRequestedFor(urlEqualTo("/huespedes/7")));
  }

  @Test
  @Order(2)
  void huespedInexistente_user404_seTraduceA404DeNegocio() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/huespedes/99"))
            .willReturn(aResponse().withStatus(404).withBody("{\"message\":\"no existe\"}")));

    RecursoNoEncontradoException e =
        assertThrows(RecursoNoEncontradoException.class, () -> remotos.obtenerHuesped(99));

    assertEquals("No existe el huésped 99", e.getMessage());
  }

  @Test
  @Order(3)
  void user500_seTraduceA503() {
    WIREMOCK.stubFor(get(urlEqualTo("/huespedes/7")).willReturn(aResponse().withStatus(500)));

    ServicioNoDisponibleException e =
        assertThrows(ServicioNoDisponibleException.class, () -> remotos.obtenerHuesped(7));

    assertTrue(e.getMessage().contains("user-svc"), e.getMessage());
  }

  @Test
  @Order(4)
  void conexionCortada_seTraduceA503() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/huespedes/7"))
            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

    assertThrows(ServicioNoDisponibleException.class, () -> remotos.obtenerHuesped(7));
  }

  @Test
  @Order(5)
  void timeout_seTraduceA503_sinColgarse() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/huespedes/7")).willReturn(okJson(HUESPED_JSON).withFixedDelay(2000)));

    long inicio = System.nanoTime();
    assertThrows(ServicioNoDisponibleException.class, () -> remotos.obtenerHuesped(7));
    long ms = (System.nanoTime() - inicio) / 1_000_000;

    // read-timeout = 500 ms: corta bastante antes de los 2000 ms que tarda el servidor
    assertTrue(ms < 1800, "debería cortar por timeout, tardó " + ms + " ms");
  }

  @Test
  @Order(99)
  void servicioCaido_puertoCerrado_seTraduceA503() {
    WIREMOCK.stop();

    ServicioNoDisponibleException e =
        assertThrows(ServicioNoDisponibleException.class, () -> remotos.obtenerHuesped(7));

    assertTrue(e.getMessage().contains("user-svc"), e.getMessage());
  }
}
