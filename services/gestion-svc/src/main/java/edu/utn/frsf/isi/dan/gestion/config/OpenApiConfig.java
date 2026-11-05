package edu.utn.frsf.isi.dan.gestion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI de gestion-svc. El Swagger UI vive en el API Gateway
 * (http://localhost:8080/swagger-ui.html) y desde ahí se llama a gestion-svc con el prefijo de su
 * ruta (/gestion): por eso el servidor declarado es "/gestion" y "Try it out" pasa por el gateway.
 *
 * <p>Para usar este servicio sin gateway (IDE) se puede dejar vacío {@code openapi.server-url}.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI(@Value("${openapi.server-url:/gestion}") String serverUrl) {
    OpenAPI api = new OpenAPI().info(new Info().title("gestion-svc").version("v1"));
    if (serverUrl != null && !serverUrl.isBlank()) {
      api.addServersItem(new Server().url(serverUrl).description("vía API Gateway"));
    }
    return api;
  }
}
