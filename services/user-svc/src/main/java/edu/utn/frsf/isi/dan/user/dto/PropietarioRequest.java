package edu.utn.frsf.isi.dan.user.dto;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PropietarioRequest(
    @NotBlank(message = "El nombre no puede estar vacío") 
    @Length(min = 2, message = "El nombre no puede tener menos de 2 caracteres")  
    String nombre,
    
    @Email(message = "El email no es válido")
    String email,
    
    @NotBlank(message = "El teléfono no puede estar vacío")
    String telefono,    
    
    @NotBlank(message = "El DNI no puede estar vacío")
    String dni,

    Long idHotel,
    
    CuentaRequest cuentaBancaria
) {}