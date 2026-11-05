package edu.utn.frsf.isi.dan.gestion.config;

import io.micrometer.observation.ObservationPredicate;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Observabilidad (Etapa 5).
 *
 * <ul>
 *   <li>Deja de generar trazas/métricas para el ruido de infraestructura: los scrapes de Prometheus
 *       a {@code /actuator/**} (cada 15 s por instancia) y los heartbeats al servidor Eureka.
 *   <li>Conecta el appender de logback de OpenTelemetry con el SDK que arma Spring Boot, para que
 *       los logs viajen por OTLP con su {@code trace_id} / {@code span_id}.
 * </ul>
 */
@Configuration
public class ObservabilityConfig {

  @Bean
  public ObservationPredicate ignorarRuidoDeInfraestructura() {
    return (name, context) -> {
      if (context instanceof ServerRequestObservationContext servidor) {
        String path = servidor.getCarrier().getRequestURI();
        return !(path.equals("/actuator") || path.startsWith("/actuator/"));
      }
      if (context instanceof ClientRequestObservationContext cliente) {
        return !cliente.getCarrier().getURI().getPath().startsWith("/eureka/");
      }
      return true;
    };
  }

  /** Instala el appender de logback de OpenTelemetry (si no, no exporta nada). */
  @Bean
  public InitializingBean openTelemetryAppenderInitializer(OpenTelemetry openTelemetry) {
    return () -> OpenTelemetryAppender.install(openTelemetry);
  }
}
