package edu.utn.frsf.isi.dan.reservas_svc.exception;

public record ExceptionInfo(String message, String path, String timestamp, int status) {}
