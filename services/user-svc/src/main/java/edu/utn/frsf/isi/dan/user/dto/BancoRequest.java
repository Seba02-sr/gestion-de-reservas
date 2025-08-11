package edu.utn.frsf.isi.dan.user.dto;

import org.hibernate.validator.constraints.Length;
import jakarta.validation.constraints.NotBlank;

public record BancoRequest(
    @NotBlank(message = "El nombre no puede estar vacío")
    @Length(max = 255, message = "El nombre no puede tener mas de 255 caracteres")
    @Length(min = 2, message = "El nombre tiene que tener al menos 2 caracteres")
    String nombre
) {}
