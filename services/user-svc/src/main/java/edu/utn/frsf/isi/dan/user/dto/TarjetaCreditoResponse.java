package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

@Builder
public record TarjetaCreditoResponse(
    Integer id,
    String numero,
    String nombreTitular,
    String fechaVencimiento,
    String cvc,
    Boolean esPrincipal,
    Integer idBanco,
    Boolean activo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime fechaRegistro,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime fechaEliminado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    LocalDateTime fechaModificado
) {}
