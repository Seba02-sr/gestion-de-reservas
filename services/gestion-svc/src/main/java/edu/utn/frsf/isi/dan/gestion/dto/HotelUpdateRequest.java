package edu.utn.frsf.isi.dan.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;

/** Únicos datos editables de un hotel: categoría, teléfono y correo de contacto. */
@Builder
public record HotelUpdateRequest(
    @NotNull(message = "La categoría no puede ser nula")
        @Min(value = 0, message = "La categoría mínima es 0")
        Integer categoria,
    @NotBlank(message = "El teléfono no puede estar vacío") String telefono,
    @NotBlank(message = "El correo de contacto no puede estar vacío")
        @Length(min = 5, message = "El correo de contacto no puede tener menos de 5 caracteres")
        @Email(message = "El correo de contacto no es válido")
        String correoContacto) {}
