package edu.utn.frsf.isi.dan.user.exception;

/** El login con Google no está habilitado: falta google.oauth.clientId (GOOGLE_CLIENT_ID). */
public class GoogleNoConfiguradoException extends RuntimeException {
  public GoogleNoConfiguradoException() {
    super("El login con Google no está configurado en este entorno");
  }
}
