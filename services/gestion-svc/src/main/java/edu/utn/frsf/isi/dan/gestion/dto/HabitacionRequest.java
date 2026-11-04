package edu.utn.frsf.isi.dan.gestion.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record HabitacionRequest(
    @NotNull(message = "El número de habitación no puede ser nulo")
        @Positive(message = "El número de habitación debe ser mayor a 0")
        Integer numero,
    @NotNull(message = "El piso no puede ser nulo")
        @Positive(message = "El piso debe ser mayor a 0")
        Integer piso,
    @NotNull(message = "El tipo de habitación es obligatorio") Integer idTipoHabitacion,
    @NotNull(message = "El hotel es obligatorio") Integer idHotel,
    /** Opcional: si no viene, la habitación queda disponible. */
    Boolean disponible) {}
