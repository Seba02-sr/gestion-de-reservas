package edu.utn.frsf.isi.dan.reservas_svc.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewRequest(
    @NotNull(message = "El rating es obligatorio")
        @DecimalMin(value = "1.0", message = "El rating mínimo es 1")
        @DecimalMax(value = "5.0", message = "El rating máximo es 5")
        Double rating,
    @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
        String comentario) {}
