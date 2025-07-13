package edu.utn.frsf.isi.dan.user.dto;

import org.hibernate.validator.constraints.Length;

import edu.utn.frsf.isi.dan.user.model.Banco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BancoRecord(
    @NotBlank(message = "El nombre no puede estar vacío") 
    @NotNull(message = "El nombre no puede ser nulo")
    @Length(min = 255, message = "El nombre no puede tener menos de 255 caracteres")
    String nombre
) {
    public Banco toBanco() {
        Banco banco = new Banco();
        banco.setNombre(nombre);
        return banco;
    }
}
