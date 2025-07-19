package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDateTime;

public record CuentaResponse(
    Integer id,
    String numeroCuenta,
    String cbu,
    String alias,
    Integer idBanco,
    LocalDateTime fechaRegistro
) {}
