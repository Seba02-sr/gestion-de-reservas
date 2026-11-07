package edu.utn.frsf.isi.dan.user.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dice qué instancia respondió. Con varias réplicas ({@code docker compose up --scale}) permite ver
 * el balanceo: cada llamada por el gateway puede devolver una {@code instancia} (id del contenedor)
 * y una {@code ip} distintas.
 */
@RestController
@RequestMapping("/info")
public class InfoController {

  private final String servicio;
  private final String instancia;

  public InfoController(
      @Value("${spring.application.name}") String servicio,
      @Value("${HOSTNAME:local}") String instancia) {
    this.servicio = servicio;
    this.instancia = instancia;
  }

  @GetMapping
  public Map<String, Object> info() {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("servicio", servicio);
    info.put("instancia", instancia);
    info.put("ip", ip());
    info.put("hora", LocalDateTime.now().toString());
    return info;
  }

  private static String ip() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      return "desconocida";
    }
  }
}
