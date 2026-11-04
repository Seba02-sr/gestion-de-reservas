package edu.utn.frsf.isi.dan.reservas_svc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.Builder;

/** Bloqueo de una habitación por parte del dueño (sin huésped ni precio). */
@Builder
public record BloqueoRequest(
    @NotNull(message = "habitacionId es obligatorio")
        @Positive(message = "habitacionId debe ser mayor a 0")
        Long habitacionId,
    @NotNull(message = "checkIn es obligatorio") LocalDate checkIn,
    @NotNull(message = "checkOut es obligatorio") LocalDate checkOut) {}
