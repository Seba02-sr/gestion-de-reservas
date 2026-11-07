package edu.utn.frsf.isi.dan.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * El gateway es la puerta pública: NO expone el actuator de los servicios. Cualquier pedido a
 * {@code /<servicio>/actuator/**} (por ejemplo {@code POST /users/actuator/shutdown}) se corta acá
 * con 404 y ni siquiera llega al servicio. Prometheus no pasa por el gateway: lee las métricas
 * directo por la red de Docker.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ActuatorBlockFilter extends OncePerRequestFilter {

  /** /users/actuator, /gestion/actuator/health, /reservas/actuator/prometheus, ... */
  private static final Pattern ACTUATOR = Pattern.compile("^/[^/]+/actuator(/.*)?$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (ACTUATOR.matcher(request.getRequestURI()).matches()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    chain.doFilter(request, response);
  }
}
