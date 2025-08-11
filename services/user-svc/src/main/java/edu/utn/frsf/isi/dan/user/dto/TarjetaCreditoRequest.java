package edu.utn.frsf.isi.dan.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record TarjetaCreditoRequest(
    @NotBlank(message = "El número de tarjeta de crédito no puede estar vacío")
    @Size(min = 13, max = 19, message = "El número de tarjeta debe tener entre 13 y 19 dígitos")
    String numero,
    
    @NotBlank(message = "El nombre del titular no puede estar vacío")
    String nombreTitular,
    
    @NotBlank(message = "La fecha de vencimiento de la tarjeta no puede estar vacía")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/(\\d{2}|\\d{4})$", message = "La fecha de vencimiento debe tener formato MM/AA o MM/AAAA")
    String fechaVencimiento,
    
    @NotBlank(message = "El código CVC no puede estar vacío")
    @Size(min = 3, max = 4, message = "El código CVC debe tener 3 o 4 dígitos")
    String cvc,
    
    @NotNull(message = "Debe especificar si es la tarjeta principal")
    Boolean esPrincipal,
    
    @NotNull(message = "Debe seleccionar un banco")
    Integer idBanco
) {}
