package edu.utn.frsf.isi.dan.reservas_svc.exception;

/** El recurso pedido no existe (404). */
public class RecursoNoEncontradoException extends RuntimeException {
  public RecursoNoEncontradoException(String mensaje) {
    super(mensaje);
  }
}
