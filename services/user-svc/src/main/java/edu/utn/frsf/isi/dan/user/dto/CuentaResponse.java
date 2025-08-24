package edu.utn.frsf.isi.dan.user.dto;

public record CuentaResponse(
    Integer id,
    String numeroCuenta,
    String cbu,
    String alias,
    Integer idBanco
) {}
