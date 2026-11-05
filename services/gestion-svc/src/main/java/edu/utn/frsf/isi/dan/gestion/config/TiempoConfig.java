package edu.utn.frsf.isi.dan.gestion.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas ({@code @Scheduled}) y define el reloj de negocio.
 *
 * <p>El "hoy" de las tarifas se calcula siempre con este {@link Clock} (zona {@code app.zone}) y no
 * con el reloj del servidor, que en un contenedor suele estar en UTC. Además permite fijar la fecha
 * en los tests.
 */
@Configuration
@EnableScheduling
public class TiempoConfig {

  @Bean
  public Clock clock(@Value("${app.zone:America/Argentina/Buenos_Aires}") String zona) {
    return Clock.system(ZoneId.of(zona));
  }
}
