package edu.utn.frsf.isi.dan.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CuentaRequest(
    @NotBlank(message = "El número de cuenta no puede estar vacío")
    String numeroCuenta,
    @NotBlank(message = "El número de cuenta no puede estar vacío")
    String cbu,
    @NotBlank(message = "El número de cuenta no puede estar vacío")
    String alias,
    @NotNull(message = "El banco no puede estar vacío")
    Integer idBanco
) {}