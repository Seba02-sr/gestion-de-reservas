package edu.utn.frsf.isi.dan.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * Datos de una tarjeta que salen por la API. El {@code numero} va enmascarado ({@code **** ****
 * **** 0004}) y el código de seguridad (CVC) NUNCA se devuelve.
 */
@Builder
public record TarjetaCreditoResponse(
    Integer id,
    String numero,
    String nombreTitular,
    String fechaVencimiento,
    Boolean esPrincipal,
    Integer idBanco,
    Boolean activo,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime fechaRegistro,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime fechaEliminado,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime fechaModificado) {}
