package edu.utn.frsf.isi.dan.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Rutas {@code lb://}: el gateway resuelve el nombre lógico "gestion-svc" a varias instancias y
 * reparte las llamadas entre ellas (balanceo del lado del cliente). Para no necesitar un Eureka
 * real se usa el discovery "simple" de Spring Cloud con dos instancias falsas.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "eureka.client.enabled=false",
      // misma ruta que en application-eureka.properties (lista completa: ver ese archivo)
      "spring.cloud.gateway.server.webmvc.routes[0].id=gestion-svc",
      "spring.cloud.gateway.server.webmvc.routes[0].uri=lb://gestion-svc",
      "spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/gestion/**",
      "spring.cloud.gateway.server.webmvc.routes[0].filters[0]=StripPrefix=1",
      // ruta a un servicio que no tiene ninguna instancia registrada
      "spring.cloud.gateway.server.webmvc.routes[1].id=fantasma",
      "spring.cloud.gateway.server.webmvc.routes[1].uri=lb://servicio-fantasma",
      "spring.cloud.gateway.server.webmvc.routes[1].predicates[0]=Path=/fantasma/**",
      "spring.cloud.gateway.server.webmvc.routes[1].filters[0]=StripPrefix=1"
    })
class GatewayLoadBalancerTest {

  private static final HttpServer INSTANCIA_A = backend("instancia-A");
  private static final HttpServer INSTANCIA_B = backend("instancia-B");

  private static HttpServer backend(String nombre) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            byte[] bytes =
                (nombre + " " + exchange.getRequestURI()).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      server.start();
      return server;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @DynamicPropertySource
  static void instancias(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.discovery.client.simple.instances.gestion-svc[0].uri",
        () -> "http://127.0.0.1:" + INSTANCIA_A.getAddress().getPort());
    registry.add(
        "spring.cloud.discovery.client.simple.instances.gestion-svc[1].uri",
        () -> "http://127.0.0.1:" + INSTANCIA_B.getAddress().getPort());
  }

  @AfterAll
  static void detener() {
    INSTANCIA_A.stop(0);
    INSTANCIA_B.stop(0);
  }

  @LocalServerPort private int port;

  @Test
  void lbRepartePeticionesEntreLasInstanciasYAplicaStripPrefix() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    Set<String> instanciasQueRespondieron = new HashSet<>();

    for (int i = 0; i < 6; i++) {
      HttpResponse<String> r =
          client.send(
              HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/gestion/info"))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(200, r.statusCode());
      assertTrue(r.body().endsWith(" /info"), "StripPrefix: " + r.body());
      instanciasQueRespondieron.add(r.body().split(" ")[0]);
    }

    assertEquals(Set.of("instancia-A", "instancia-B"), instanciasQueRespondieron);
  }

  @Test
  void sinInstanciasRegistradas_responde503_noUn500() throws Exception {
    HttpResponse<String> r =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/fantasma/x"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());

    assertEquals(503, r.statusCode());
    assertTrue(r.body().contains("Servicio no disponible"), r.body());
  }
}
