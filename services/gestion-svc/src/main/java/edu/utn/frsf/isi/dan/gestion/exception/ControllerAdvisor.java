package edu.utn.frsf.isi.dan.gestion.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ControllerAdvisor {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ExceptionInfo> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(),
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ExceptionInfo> handleEntityNotFoundException(
      EntityNotFoundException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(),
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.NOT_FOUND.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ExceptionInfo> handleIllegalStateException(
      IllegalStateException ex, WebRequest request) {
    return conflict(ex.getMessage(), request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ExceptionInfo> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, WebRequest request) {
    // p. ej. UNIQUE (id_hotel, numero) de habitacion ante dos altas concurrentes
    return conflict("La operación viola una restricción de integridad de datos", request);
  }

  private ResponseEntity<ExceptionInfo> conflict(String message, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            message,
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.CONFLICT.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.CONFLICT);
  }

  /** JSON mal formado o con un valor que no se puede convertir (p. ej. una fecha inválida). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ExceptionInfo> handleMessageNotReadable(
      HttpMessageNotReadableException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            "El cuerpo de la solicitud no es válido (JSON mal formado o formato de dato incorrecto)",
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  /** Un parámetro o variable de la URL con tipo incorrecto (p. ej. fecha=hola). */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ExceptionInfo> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            "El parámetro '" + ex.getName() + "' tiene un formato inválido",
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  /** Por ejemplo PUT sobre /tarifas: las tarifas no se modifican. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ExceptionInfo> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(),
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.METHOD_NOT_ALLOWED.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionInfo> handleGeneralException(Exception ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(),
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.INTERNAL_SERVER_ERROR.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ExceptionInfo> handleValidationException(
      MethodArgumentNotValidException ex, WebRequest request) {
    String errorMessage =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .reduce((message1, message2) -> message1 + ", " + message2)
            .orElse("Validation error");

    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            errorMessage,
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ExceptionInfo> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    String errorMessage =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .reduce((message1, message2) -> message1 + ", " + message2)
            .orElse("Constraint violation error");

    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            errorMessage,
            request.getDescription(false),
            String.valueOf(System.currentTimeMillis()),
            HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }
}
