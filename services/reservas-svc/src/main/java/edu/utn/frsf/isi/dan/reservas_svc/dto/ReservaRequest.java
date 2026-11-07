package edu.utn.frsf.isi.dan.reservas_svc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Builder;

/** Alta de reserva. {@code habitacionId} es el id de la habitación en gestion-svc. */
@Builder
public record ReservaRequest(
    @NotNull(message = "habitacionId es obligatorio")
        @Positive(message = "habitacionId debe ser mayor a 0")
        Long habitacionId,
    @NotNull(message = "idHuesped es obligatorio")
        @Positive(message = "idHuesped debe ser mayor a 0")
        Integer idHuesped,
    @NotNull(message = "checkIn es obligatorio") LocalDate checkIn,
    @NotNull(message = "checkOut es obligatorio") LocalDate checkOut) {}
