package edu.utn.frsf.isi.dan.gestion.scheduling;

import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tarea programada (y chequeo al arrancar): una vez por día avisa a reservas-svc de los tipos de
 * habitación cuyo precio vigente cambió ese día (una promoción que empieza, o que termina y vuelve
 * el precio común).
 *
 * <p>El cron se configura con {@code tarifas.cron-cambio-precio} (por defecto {@code 0 5 0 * * *},
 * todos los días a las 00:05) y la zona horaria con {@code app.zone}.
 */
@Component
@Log4j2
public class TarifaPrecioScheduler {

  @Autowired private TarifaService tarifaService;

  /**
   * Hace el mismo chequeo una vez cuando la aplicación termina de arrancar, por si estuvo apagada a
   * las 00:05. Si ya se había publicado ese día se repite el mismo precio, lo cual es inofensivo
   * (reservas-svc solo pisa el precioNoche con el valor vigente).
   */
  @EventListener(ApplicationReadyEvent.class)
  public void alArrancar() {
    log.info("Aplicación lista: chequeando cambios de precio del día");
    publicarCambiosDePrecio();
  }

  @Scheduled(
      cron = "${tarifas.cron-cambio-precio:0 5 0 * * *}",
      zone = "${app.zone:America/Argentina/Buenos_Aires}")
  public void publicarCambiosDePrecio() {
    try {
      int tipos = tarifaService.publicarCambiosDePrecioDelDia();
      log.info("Tarea diaria de precios: {} tipo(s) de habitación con cambio de precio", tipos);
    } catch (Exception e) {
      // una excepción no cancela las próximas ejecuciones, pero conviene dejarla registrada
      log.error("Falló la tarea diaria de cambios de precio: {}", e.getMessage(), e);
    }
  }
}
