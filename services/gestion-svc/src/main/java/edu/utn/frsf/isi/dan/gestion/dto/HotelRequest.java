package edu.utn.frsf.isi.dan.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Mod11Check;

@Builder
public record HotelRequest(
    @NotBlank(message = "El nombre no puede estar vacío")
        @Length(min = 2, message = "El nombre no puede tener menos de 2 caracteres")
        String nombre,
    @NotBlank(message = "El CUIT no puede estar vacío")
        @Mod11Check(
            threshold = 7,
            message = "El CUIT no es válido") // permite verificar el CUIT / CUIL argentino
        String cuit,
    @NotBlank(message = "El domicilio no puede estar vacío")
        @Length(min = 5, message = "El domicilio no puede tener menos de 5 caracteres")
        String domicilio,
    @NotNull(message = "La latitud no puede ser nula")
        @DecimalMin(value = "0.0", inclusive = true, message = "La latitud mínima es 0")
        Double latitud,
    @NotNull(message = "La longitud no puede ser nula")
        @DecimalMin(value = "0.0", inclusive = true, message = "La longitud mínima es 0")
        Double longitud,
    @NotBlank(message = "El teléfono no puede estar vacío") String telefono,
    @NotBlank(message = "El correo de contacto no puede estar vacío")
        @Length(min = 5, message = "El correo de contacto no puede tener menos de 5 caracteres")
        @Email(message = "El correo de contacto no es válido")
        String correoContacto,
    @NotNull(message = "La categoría no puede ser nula")
        @Min(value = 0, message = "La categoría mínima es 0")
        Integer categoria) {}
