package edu.utn.frsf.isi.dan.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record TipoHabitacionRequest(
    @NotBlank(message = "El nombre no puede estar vacío")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres")
        String nombre,
    @NotBlank(message = "La descripción no puede estar vacía")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,
    @NotNull(message = "La capacidad es obligatoria")
        @Positive(message = "La capacidad debe ser mayor a 0")
        Integer capacidad) {}
