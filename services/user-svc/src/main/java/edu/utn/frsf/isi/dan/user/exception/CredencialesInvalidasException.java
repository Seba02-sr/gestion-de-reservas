package edu.utn.frsf.isi.dan.user.exception;

/** Usuario o contraseña incorrectos. El mensaje es genérico a propósito (no revela cuál falló). */
public class CredencialesInvalidasException extends RuntimeException {
  public CredencialesInvalidasException() {
    super("Usuario o contraseña incorrectos");
  }
}
