package edu.utn.frsf.isi.dan.reservas_svc.exception;

/**
 * Un servicio del que depende la operación no responde, tardó demasiado o su circuito está abierto
 * (503). No es un error del cliente ni un bug: es reintentable.
 */
public class ServicioNoDisponibleException extends RuntimeException {
  public ServicioNoDisponibleException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
