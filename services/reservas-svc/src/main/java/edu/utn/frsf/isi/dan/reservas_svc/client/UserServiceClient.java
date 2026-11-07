package edu.utn.frsf.isi.dan.reservas_svc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo de user-svc: Feign genera la implementación a partir de esta interfaz.
 * {@code name} es el nombre con el que user-svc se registra en Eureka (el balanceador elige la
 * instancia); si {@code clients.user-svc.url} está definida se usa esa URL fija (tests, IDE).
 */
@FeignClient(name = "user-svc", url = "${clients.user-svc.url:}")
public interface UserServiceClient {

  @GetMapping("/huespedes/{id}")
  HuespedInfo getHuesped(@PathVariable("id") Integer id);
}
