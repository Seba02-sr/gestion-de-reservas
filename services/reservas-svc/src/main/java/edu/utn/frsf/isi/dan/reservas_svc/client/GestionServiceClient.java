package edu.utn.frsf.isi.dan.reservas_svc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Cliente HTTP declarativo de gestion-svc (fuente de verdad de hoteles y habitaciones). */
@FeignClient(name = "gestion-svc", url = "${clients.gestion-svc.url:}")
public interface GestionServiceClient {

  /** Id de la habitación en gestion-svc. */
  @GetMapping("/habitaciones/{id}/disponible")
  Boolean habitacionDisponible(@PathVariable("id") Integer id);
}
