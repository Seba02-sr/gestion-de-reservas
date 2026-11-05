package edu.utn.frsf.isi.dan.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

/**
 * Datos actualizables de un huésped (consigna ETAPA01): datos de usuario y fecha de nacimiento.
 * Todos los campos son opcionales; solo se validan formato si vienen informados. No incluye
 * password, username ni tarjetas.
 */
@Builder
public record HuespedUpdateRequest(
    @Length(min = 2, message = "El nombre no puede tener menos de 2 caracteres") String nombre,
    @Email(message = "El email no es válido") String email,
    String telefono,
    String dni,
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
        LocalDate fechaNacimiento) {}
