package edu.utn.frsf.isi.dan.reservas_svc.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones de negocio a 400 / 404 / 409 / 503 (y deja el 500 solo para lo
 * inesperado).
 */
@RestControllerAdvice
@Log4j2
public class ControllerAdvisor {

  @ExceptionHandler(RecursoNoEncontradoException.class)
  public ResponseEntity<ExceptionInfo> handleNoEncontrado(
      RecursoNoEncontradoException ex, WebRequest request) {
    return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  /** user-svc / gestion-svc no responden o su circuito está abierto: 503, no un 500 genérico. */
  @ExceptionHandler(ServicioNoDisponibleException.class)
  public ResponseEntity<ExceptionInfo> handleServicioNoDisponible(
      ServicioNoDisponibleException ex, WebRequest request) {
    return respuesta(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
  }

  @ExceptionHandler({ConflictoException.class, IllegalStateException.class})
  public ResponseEntity<ExceptionInfo> handleConflicto(RuntimeException ex, WebRequest request) {
    return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ExceptionInfo> handleArgumentoInvalido(
      IllegalArgumentException ex, WebRequest request) {
    return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  /** Validación de @RequestBody (MethodArgumentNotValidException) y de @ModelAttribute. */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<ExceptionInfo> handleBind(BindException ex, WebRequest request) {
    String mensaje =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Datos inválidos");
    return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ExceptionInfo> handleConstraint(
      ConstraintViolationException ex, WebRequest request) {
    String mensaje =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Datos inválidos");
    return respuesta(HttpStatus.BAD_REQUEST, mensaje, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ExceptionInfo> handleCuerpoIlegible(
      HttpMessageNotReadableException ex, WebRequest request) {
    return respuesta(
        HttpStatus.BAD_REQUEST,
        "El cuerpo de la solicitud no es válido (JSON mal formado o formato de dato incorrecto)",
        request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ExceptionInfo> handleTipoIncorrecto(
      MethodArgumentTypeMismatchException ex, WebRequest request) {
    return respuesta(
        HttpStatus.BAD_REQUEST,
        "El parámetro '" + ex.getName() + "' tiene un formato inválido",
        request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ExceptionInfo> handleMetodoNoPermitido(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {
    return respuesta(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionInfo> handleInesperado(Exception ex, WebRequest request) {
    log.error("Error inesperado: {}", ex.getMessage(), ex);
    return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
  }

  private ResponseEntity<ExceptionInfo> respuesta(
      HttpStatus status, String mensaje, WebRequest request) {
    ExceptionInfo info =
        new ExceptionInfo(
            mensaje, request.getDescription(false), Instant.now().toString(), status.value());
    return new ResponseEntity<>(info, status);
  }
}
