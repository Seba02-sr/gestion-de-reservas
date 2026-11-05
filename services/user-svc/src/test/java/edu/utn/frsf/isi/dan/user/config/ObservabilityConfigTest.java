package edu.utn.frsf.isi.dan.user.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.observation.ObservationPredicate;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** El ruido de infraestructura (scrapes de Prometheus, heartbeats a Eureka) no genera trazas. */
class ObservabilityConfigTest {

  private final ObservationPredicate predicado =
      new ObservabilityConfig().ignorarRuidoDeInfraestructura();

  private static ServerRequestObservationContext servidor(String uri) {
    return new ServerRequestObservationContext(
        new MockHttpServletRequest("GET", uri), new MockHttpServletResponse());
  }

  private static ClientRequestObservationContext cliente(String url) {
    ClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create(url));
    return new ClientRequestObservationContext(request);
  }

  @Test
  void ignoraLosPedidosAlActuator() {
    assertFalse(predicado.test("http.server.requests", servidor("/actuator/prometheus")));
    assertFalse(predicado.test("http.server.requests", servidor("/actuator/health")));
  }

  @Test
  void noIgnoraLosPedidosDeNegocio() {
    assertTrue(predicado.test("http.server.requests", servidor("/huespedes/5")));
    assertTrue(predicado.test("http.server.requests", servidor("/actuatorx")));
  }

  @Test
  void ignoraLosHeartbeatsAEureka_peroNoLasLlamadasDeNegocio() {
    assertFalse(
        predicado.test(
            "http.client.requests", cliente("http://dan-eureka-server:8761/eureka/apps/X")));
    assertTrue(predicado.test("http.client.requests", cliente("http://user-svc/huespedes/5")));
  }
}
