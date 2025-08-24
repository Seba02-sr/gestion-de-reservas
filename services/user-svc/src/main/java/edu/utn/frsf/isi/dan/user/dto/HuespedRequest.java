package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

public record HuespedRequest(
    @NotBlank(message = "El nombre no puede estar vacío")     
    @Length(min = 2, message = "El nombre no puede tener menos de 2 caracteres")  
    String nombre,
    
    @Email(message = "El email no es válido")
    String email,
    
    @NotBlank(message = "El teléfono no puede estar vacío")
    String telefono,
    
    @NotBlank(message = "El DNI no puede estar vacío")
    String dni,
    
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    LocalDate fechaNacimiento,
    
    @Valid
    @NotEmpty(message = "Debe tener al menos una tarjeta de crédito")
    List<TarjetaCreditoRequest> tarjetasCredito
) {}
