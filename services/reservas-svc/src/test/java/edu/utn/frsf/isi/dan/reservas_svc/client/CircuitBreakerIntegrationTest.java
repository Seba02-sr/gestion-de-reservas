package edu.utn.frsf.isi.dan.reservas_svc.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Circuit breaker de Resilience4j alrededor de los clients Feign: tras varios fallos el circuito se
 * abre y deja de llamar al servicio (503 inmediato); un 404 no cuenta como fallo; pasado el tiempo
 * de espera se prueba de nuevo (half-open) y, si el servicio volvió, se cierra.
 */
@SpringBootTest(
    properties = {
      "reservas.crear-indices=false",
      "spring.rabbitmq.listener.simple.auto-startup=false",
      "spring.cloud.openfeign.client.config.default.read-timeout=500",
      // ventana chica y espera corta para que la prueba sea rápida
      "resilience4j.circuitbreaker.configs.default.sliding-window-size=4",
      "resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=4",
      "resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50",
      "resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=1s",
      "resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state=1"
    })
class CircuitBreakerIntegrationTest {

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

  @Autowired private ServiciosRemotos remotos;

  @BeforeEach
  void limpiar() {
    WIREMOCK.resetAll();
  }

  @Test
  void tras_variosFallos_elCircuitoSeAbre_yNoSeLlamaMasAlServicio_yAlVolverSeCierra() {
    WIREMOCK.stubFor(get(urlEqualTo("/huespedes/7")).willReturn(aResponse().withStatus(500)));

    // 4 llamadas fallidas = ventana completa con 100 % de fallos -> el circuito se abre
    for (int i = 0; i < 4; i++) {
      assertThrows(ServicioNoDisponibleException.class, () -> remotos.obtenerHuesped(7));
    }
    WIREMOCK.verify(4, getRequestedFor(urlEqualTo("/huespedes/7")));

    // circuito abierto: 503 inmediato, SIN tocar el servicio
    ServicioNoDisponibleException abierto =
        assertThrows(ServicioNoDisponibleException.class, () -> remotos.obtenerHuesped(7));
    WIREMOCK.verify(4, getRequestedFor(urlEqualTo("/huespedes/7")));
    assertTrue(causaEs(abierto, CallNotPermittedException.class), "debería ser circuito abierto");

    // el servicio se recupera: tras la espera (half-open) la llamada de prueba sale bien y cierra
    WIREMOCK.resetAll();
    WIREMOCK.stubFor(
        get(urlEqualTo("/huespedes/7"))
            .willReturn(okJson("{\"id\":7,\"nombre\":\"Carla\",\"email\":\"carla@example.com\"}")));
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .ignoreExceptions() // mientras el circuito sigue abierto la llamada lanza 503
        .untilAsserted(() -> assertEquals("Carla", remotos.obtenerHuesped(7).nombre()));
    // y sigue funcionando (circuito cerrado)
    assertEquals("Carla", remotos.obtenerHuesped(7).nombre());
  }

  @Test
  void los404NoCuentanComoFallo_elCircuitoNoSeAbre() {
    WIREMOCK.stubFor(
        get(urlEqualTo("/habitaciones/55/disponible")).willReturn(aResponse().withStatus(404)));

    for (int i = 0; i < 12; i++) {
      assertThrows(RecursoNoEncontradoException.class, () -> remotos.habitacionDisponible(55L));
    }

    // las 12 llamadas llegaron al servicio: nunca se cortó el paso
    WIREMOCK.verify(12, getRequestedFor(urlEqualTo("/habitaciones/55/disponible")));
  }

  private static boolean causaEs(Throwable t, Class<? extends Throwable> tipo) {
    for (int i = 0; t != null && i < 10; i++, t = t.getCause()) {
      if (tipo.isInstance(t)) {
        return true;
      }
    }
    return false;
  }
}
