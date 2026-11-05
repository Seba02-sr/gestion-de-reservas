package edu.utn.frsf.isi.dan.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.context.ShutdownEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.management.ThreadDumpEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

/**
 * Seguridad del actuator de user-svc: con la configuración REAL de application.properties solo se
 * exponen health, info y prometheus, y el shutdown directamente no existe. Se cargan las
 * auto-configuraciones de los endpoints "peligrosos" para comprobar que la configuración los apaga.
 */
class ActuatorExposureTest {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          // igual que SpringApplication: permite convertir valores como "none" al enum Access
          .withInitializer(
              ctx -> ctx.getEnvironment().setConversionService(new ApplicationConversionService()))
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withConfiguration(
              AutoConfigurations.of(
                  EndpointAutoConfiguration.class,
                  WebEndpointAutoConfiguration.class,
                  ShutdownEndpointAutoConfiguration.class,
                  EnvironmentEndpointAutoConfiguration.class,
                  BeansEndpointAutoConfiguration.class,
                  ThreadDumpEndpointAutoConfiguration.class,
                  HeapDumpWebEndpointAutoConfiguration.class,
                  MetricsEndpointAutoConfiguration.class,
                  ConditionsReportEndpointAutoConfiguration.class,
                  HealthContributorAutoConfiguration.class,
                  HealthEndpointAutoConfiguration.class,
                  InfoContributorAutoConfiguration.class,
                  InfoEndpointAutoConfiguration.class));

  private static Set<String> idsExpuestos(WebEndpointsSupplier supplier) {
    return supplier.getEndpoints().stream()
        .map(e -> e.getEndpointId().toLowerCaseString())
        .collect(Collectors.toSet());
  }

  @Test
  void soloSeExponenHealthInfoYPrometheus_yNoHayShutdown() {
    runner.run(
        context -> {
          Set<String> ids = idsExpuestos(context.getBean(WebEndpointsSupplier.class));
          assertThat(ids).isNotEmpty().isSubsetOf("health", "info", "prometheus");
          assertThat(ids)
              .doesNotContain(
                  "shutdown", "env", "beans", "heapdump", "threaddump", "metrics", "conditions");
        });
  }

  @Test
  void elShutdownNoEstaHabilitadoNiPorAccesoNiPorExposicion() {
    runner.run(
        context -> {
          assertThat(context.getEnvironment().getProperty("management.endpoint.shutdown.access"))
              .isEqualTo("none");
          assertThat(
                  context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
              .isEqualTo("health,info,prometheus");
        });
  }
}
