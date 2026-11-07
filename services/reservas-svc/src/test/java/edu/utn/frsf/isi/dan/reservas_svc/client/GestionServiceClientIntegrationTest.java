package edu.utn.frsf.isi.dan.reservas_svc.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** El client Feign real contra un gestion-svc simulado con WireMock. */
@SpringBootTest(
    properties = {
      "reservas.crear-indices=false",
      "spring.rabbitmq.listener.simple.auto-startup=false",
      "spring.cloud.openfeign.client.config.default.connect-timeout=500",
      "spring.cloud.openfeign.client.config.default.read-timeout=500",
      "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=1000"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GestionServiceClientIntegrationTest {

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

  @Autowired private GestionServiceClient client;
  @Autowired private ServiciosRemotos remotos;

  @BeforeEach
  void limpiar() {
    WIREMOCK.resetAll();
  }

  @Test
  @Order(1)
  void disponible_true() {
    WIREMOCK.stubFor(get(urlEqualTo("/habitaciones/10/disponible")).willReturn(okJson("true")));

    assertTrue(client.habitacionDisponible(10));
    assertTrue(remotos.habitacionDisponible(10L));
  }

  @Test
  @Order(2)
  void disponible_false_porHotelCerradoOHabitacionNoDisponible() {
    WIREMOCK.stubFor(get(urlEqualTo("/habitaciones/10/disponible")).willReturn(okJson("false")));

    assertFalse(remotos.habitacionDisponible(10L));
  }

  @Test
  @Order(3)
  void habitacionInexistente_gestion404_seTraduceA404DeNegocio() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/habitaciones/55/disponible")).willReturn(aResponse().withStatus(404)));

    assertThrows(RecursoNoEncontradoException.class, () -> remotos.habitacionDisponible(55L));
  }

  @Test
  @Order(4)
  void gestion500_seTraduceA503() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/habitaciones/10/disponible")).willReturn(aResponse().withStatus(500)));

    ServicioNoDisponibleException e =
        assertThrows(ServicioNoDisponibleException.class, () -> remotos.habitacionDisponible(10L));

    assertTrue(e.getMessage().contains("gestion-svc"), e.getMessage());
  }

  @Test
  @Order(5)
  void conexionCortada_seTraduceA503() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/habitaciones/10/disponible"))
            .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

    assertThrows(ServicioNoDisponibleException.class, () -> remotos.habitacionDisponible(10L));
  }

  @Test
  @Order(99)
  void servicioCaido_puertoCerrado_seTraduceA503() {
    WIREMOCK.stop();

    assertThrows(ServicioNoDisponibleException.class, () -> remotos.habitacionDisponible(10L));
  }
}
