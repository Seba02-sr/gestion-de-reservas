package edu.utn.frsf.isi.dan.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Levanta el gateway real contra un backend falso (eco del método, path y body) y comprueba el
 * StripPrefix, que el body de un POST llega intacto y el CORS.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

  private static final HttpServer BACKEND = crearBackend();

  private static HttpServer crearBackend() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/",
          exchange -> {
            String body =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String eco = exchange.getRequestMethod() + " " + exchange.getRequestURI() + " " + body;
            byte[] bytes = eco.getBytes(StandardCharsets.UTF_8);
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

  private HttpResponse<String> enviar(HttpRequest.Builder b) throws Exception {
    return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  @Test
  void gestion_quitaElPrefijo() throws Exception {
    HttpResponse<String> r = enviar(HttpRequest.newBuilder(uri("/gestion/hoteles?x=1")).GET());
    assertEquals(200, r.statusCode());
    assertEquals("GET /hoteles?x=1 ", r.body());
  }

  @Test
  void users_quitaSoloElPrimerSegmento() throws Exception {
    HttpResponse<String> r = enviar(HttpRequest.newBuilder(uri("/users/users/huesped")).GET());
    assertEquals("GET /users/huesped ", r.body());
  }

  @Test
  void reservas_postConservaElBody() throws Exception {
    HttpResponse<String> r =
        enviar(
            HttpRequest.newBuilder(uri("/reservas/reservas"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"a\":1}")));
    assertEquals(200, r.statusCode());
    assertEquals("POST /reservas {\"a\":1}", r.body());
  }

  @Test
  void rutaDesconocida_da404() throws Exception {
    assertEquals(404, enviar(HttpRequest.newBuilder(uri("/otra/cosa")).GET()).statusCode());
  }

  @Test
  void preflightCors_conOrigenPermitido() throws Exception {
    HttpResponse<String> r =
        enviar(
            HttpRequest.newBuilder(uri("/users/auth/google"))
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
    assertEquals(200, r.statusCode());
    assertEquals(
        "http://localhost:5173",
        r.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
  }
}
