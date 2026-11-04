package edu.utn.frsf.isi.dan.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Builder;

/**
 * Alta de tarifa.
 *
 * <ul>
 *   <li>Sin {@code fechaInicio} ni {@code fechaFin}: tarifa común que rige desde hoy y reemplaza a
 *       la actual.
 *   <li>Con ambas fechas: tarifa especial/promocional para ese período.
 * </ul>
 */
@Builder
public record TarifaRequest(
    @NotBlank(message = "El nombre de la tarifa es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,
    @NotNull(message = "El tipo de habitación es obligatorio") Integer idTipoHabitacion,
    @NotNull(message = "El precio por noche es obligatorio")
        @Positive(message = "El precio por noche debe ser mayor a 0")
        Double precioNoche,
    LocalDate fechaInicio,
    LocalDate fechaFin) {}
