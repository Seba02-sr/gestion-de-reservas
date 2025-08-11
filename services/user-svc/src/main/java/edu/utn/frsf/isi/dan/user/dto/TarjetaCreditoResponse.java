package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDateTime;

public record TarjetaCreditoResponse(
    Integer id,
    String numero,
    String nombreTitular,
    String fechaVencimiento,
    String cvc,
    Boolean esPrincipal,
    Integer idBanco,
    LocalDateTime fechaRegistro,
    Boolean activo
) {}
