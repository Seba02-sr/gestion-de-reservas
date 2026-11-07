package edu.utn.frsf.isi.dan.gateway;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Cuando una ruta {@code lb://servicio} no encuentra ninguna instancia registrada en Eureka, el
 * balanceador lanza una {@link HttpServerErrorException} 503. Sin este handler el cliente vería un
 * 500 genérico; así recibe el 503 real (servicio no disponible) con un mensaje claro.
 */
@RestControllerAdvice
public class GatewayErrorAdvice {

  @ExceptionHandler(HttpServerErrorException.class)
  public ResponseEntity<Map<String, Object>> servicioNoDisponible(
      HttpServerErrorException ex, HttpServletRequest request) {
    Map<String, Object> cuerpo = new LinkedHashMap<>();
    cuerpo.put("message", "Servicio no disponible: " + ex.getStatusText());
    cuerpo.put("path", request.getRequestURI());
    cuerpo.put("timestamp", Instant.now().toString());
    cuerpo.put("status", ex.getStatusCode().value());
    return ResponseEntity.status(ex.getStatusCode()).body(cuerpo);
  }
}
