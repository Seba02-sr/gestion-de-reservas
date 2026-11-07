package edu.utn.frsf.isi.dan.reservas_svc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PagoRequest(
    @NotBlank(message = "El método de pago es obligatorio") String method,
    @NotNull(message = "El monto es obligatorio") @Positive(message = "El monto debe ser mayor a 0")
        Double monto,
    String transactionId) {}
