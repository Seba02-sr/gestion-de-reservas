package edu.utn.frsf.isi.dan.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

@Builder
public record PropietarioRequest(
    @NotBlank(message = "El nombre no puede estar vacío")
        @Length(min = 2, message = "El nombre no puede tener menos de 2 caracteres")
        String nombre,
    @Email(message = "El email no es válido") String email,
    @NotBlank(message = "El teléfono no puede estar vacío") String telefono,
    @NotBlank(message = "El DNI no puede estar vacío") String dni,
    @NotBlank(message = "El nombre de usuario es obligatorio") String username,
    @NotBlank(message = "La contraseña no puede estar vacía") String password,
    @NotNull(message = "El ID del hotel es obligatorio") Long idHotel,
    @NotNull(message = "La cuenta bancaria es obligatoria") CuentaRequest cuentaBancaria) {}
