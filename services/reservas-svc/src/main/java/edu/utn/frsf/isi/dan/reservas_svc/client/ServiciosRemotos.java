package edu.utn.frsf.isi.dan.reservas_svc.client;

import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ServicioNoDisponibleException;
import feign.FeignException;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * Único lugar donde se llama a otros servicios y se traducen sus fallos:
 *
 * <ul>
 *   <li>el servicio respondió 404 → {@link RecursoNoEncontradoException} (404 de negocio);
 *   <li>cualquier otro fallo (no responde, timeout, 5xx, sin instancias en Eureka, circuito
 *       abierto) → {@link ServicioNoDisponibleException} (503 con mensaje claro), nunca un 500.
 * </ul>
 */
@Component
@Log4j2
public class ServiciosRemotos {

  private final UserServiceClient userClient;
  private final GestionServiceClient gestionClient;

  public ServiciosRemotos(UserServiceClient userClient, GestionServiceClient gestionClient) {
    this.userClient = userClient;
    this.gestionClient = gestionClient;
  }

  /** Datos del huésped en user-svc (la fuente de verdad de los usuarios). */
  public HuespedInfo obtenerHuesped(Integer idHuesped) {
    HuespedInfo huesped =
        llamar(
            "user-svc",
            "validar el huésped " + idHuesped,
            "No existe el huésped " + idHuesped,
            () -> userClient.getHuesped(idHuesped));
    if (huesped == null) {
      throw new RecursoNoEncontradoException("No existe el huésped " + idHuesped);
    }
    return huesped;
  }

  /** Si gestion-svc (fuente de verdad) dice que la habitación está disponible. */
  public boolean habitacionDisponible(Long habitacionId) {
    Boolean disponible =
        llamar(
            "gestion-svc",
            "confirmar la disponibilidad de la habitación " + habitacionId,
            "No existe la habitación " + habitacionId + " en gestion-svc",
            () -> gestionClient.habitacionDisponible(Math.toIntExact(habitacionId)));
    if (disponible == null) {
      throw new ServicioNoDisponibleException(
          "gestion-svc devolvió una respuesta inválida al confirmar la disponibilidad de la"
              + " habitación "
              + habitacionId,
          null);
    }
    return disponible;
  }

  private <T> T llamar(
      String servicio, String accion, String mensajeNoExiste, Supplier<T> llamada) {
    try {
      return llamada.get();
    } catch (RuntimeException e) {
      if (esNotFound(e)) {
        throw new RecursoNoEncontradoException(mensajeNoExiste);
      }
      log.warn("Falló la llamada a {} para {}: {}", servicio, accion, e.toString());
      throw new ServicioNoDisponibleException(
          "No se pudo " + accion + ": " + servicio + " no está disponible. Intente nuevamente.", e);
    }
  }

  /** Busca un 404 en la cadena de causas (con circuit breaker llega envuelto). */
  private static boolean esNotFound(Throwable t) {
    for (int i = 0; t != null && i < 10; i++, t = t.getCause()) {
      if (t instanceof FeignException.NotFound) {
        return true;
      }
    }
    return false;
  }
}
