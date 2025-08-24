package edu.utn.frsf.isi.dan.user.dto;

public record TarjetaCreditoResponse(
    Integer id,
    String numero,
    String nombreTitular,
    String fechaVencimiento,
    String cvc,
    Boolean esPrincipal,
    Integer idBanco
) {}
