package edu.utn.frsf.isi.dan.user.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ExceptionInfo> handleIllegalArgumentException(
      IllegalArgumentException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(), request.getDescription(false), now(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ExceptionInfo> handleEntityNotFoundException(
      EntityNotFoundException ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(), request.getDescription(false), now(), HttpStatus.NOT_FOUND.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ExceptionInfo> handleGeneralException(Exception ex, WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            ex.getMessage(),
            request.getDescription(false),
            now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      org.springframework.http.HttpStatusCode status,
      WebRequest request) {
    String errorMessage =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Validation error");

    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            errorMessage, request.getDescription(false), now(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      org.springframework.http.HttpStatusCode status,
      WebRequest request) {
    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            "Malformed JSON request",
            request.getDescription(false),
            now(),
            HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ExceptionInfo> handleConstraintViolationException(
      ConstraintViolationException ex, WebRequest request) {
    String errorMessage =
        ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Constraint violation error");

    ExceptionInfo exceptionInfo =
        new ExceptionInfo(
            errorMessage, request.getDescription(false), now(), HttpStatus.BAD_REQUEST.value());
    return new ResponseEntity<>(exceptionInfo, HttpStatus.BAD_REQUEST);
  }

  private String now() {
    return java.time.Instant.now().toString();
  }
}
