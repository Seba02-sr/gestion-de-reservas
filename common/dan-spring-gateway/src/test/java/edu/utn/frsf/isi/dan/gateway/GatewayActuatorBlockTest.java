package edu.utn.frsf.isi.dan.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Seguridad: el gateway no debe enrutar el actuator de los servicios (por ejemplo, nadie puede
 * apagar user-svc con {@code POST /users/actuator/shutdown}). El backend falso cuenta cuántos
 * pedidos le llegan: para estas rutas tienen que ser 0.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayActuatorBlockTest {

  private static final AtomicInteger PEDIDOS_AL_BACKEND = new AtomicInteger();
  private static final HttpServer BACKEND = crearBackend();

  private static HttpServer crearBackend() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            PEDIDOS_AL_BACKEND.incrementAndGet();
            byte[] bytes = "backend".getBytes(StandardCharsets.UTF_8);
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

  @AfterAll
  static void detener() {
    BACKEND.stop(0);
  }

  @DynamicPropertySource
  static void rutas(DynamicPropertyRegistry registry) {
    String url = "http://127.0.0.1:" + BACKEND.getAddress().getPort();
    registry.add("USER_SVC_URL", () -> url);
    registry.add("GESTION_SVC_URL", () -> url);
    registry.add("RESERVAS_SVC_URL", () -> url);
  }

  @LocalServerPort private int port;

  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void reiniciarContador() {
    PEDIDOS_AL_BACKEND.set(0);
  }

  private int enviar(String metodo, String path) throws Exception {
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method(metodo, HttpRequest.BodyPublishers.noBody());
    return client.send(b.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
  }

  @Test
  void shutdownPorElGateway_noSeEnruta_yNoLlegaAlServicio() throws Exception {
    assertEquals(404, enviar("POST", "/users/actuator/shutdown"));
    assertEquals(404, enviar("POST", "/gestion/actuator/shutdown"));
    assertEquals(404, enviar("POST", "/reservas/actuator/shutdown"));
    assertEquals(0, PEDIDOS_AL_BACKEND.get());
  }

  @Test
  void ningunEndpointDeActuator_sePuedeAlcanzarPorElGateway() throws Exception {
    for (String path :
        List.of(
            "/users/actuator",
            "/users/actuator/",
            "/users/actuator/env",
            "/users/actuator/prometheus",
            "/gestion/actuator/health",
            "/gestion/actuator/heapdump",
            "/reservas/actuator/info",
            "/reservas/actuator/health/liveness")) {
      assertEquals(404, enviar("GET", path), path);
    }
    assertEquals(0, PEDIDOS_AL_BACKEND.get());
  }

  @Test
  void lasRutasDeNegocioSiguenFuncionando() throws Exception {
    assertEquals(200, enviar("GET", "/gestion/hoteles"));
    assertEquals(200, enviar("GET", "/users/users/huesped"));
    // un path parecido pero que NO es el actuator pasa normalmente
    assertEquals(200, enviar("GET", "/reservas/actuatorx/lo-que-sea"));
    assertEquals(3, PEDIDOS_AL_BACKEND.get());
  }
}
