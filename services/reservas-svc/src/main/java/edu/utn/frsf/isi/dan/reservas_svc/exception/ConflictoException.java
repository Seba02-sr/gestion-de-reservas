package edu.utn.frsf.isi.dan.reservas_svc.exception;

/**
 * La operación choca con el estado actual del recurso (409): transición inválida, fechas ocupadas.
 */
public class ConflictoException extends RuntimeException {
  public ConflictoException(String mensaje) {
    super(mensaje);
  }
}
