package edu.utn.frsf.isi.dan.user.dto;

import org.hibernate.validator.constraints.Length;

import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.model.CuentaBancaria;
import edu.utn.frsf.isi.dan.user.model.Propietario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PropietarioRecord(
    @NotBlank(message = "El nombre no puede estar vacío") 
    @Length(min = 5, message = "El nombre no puede tener menos de 5 caracteres")  
    String nombre,
    @Email(message = "El email no es válido")
    String email,
    @NotBlank(message = "El teléfono no puede estar vacío")
    String telefono,
    @NotBlank(message = "El DNI no puede estar vacío")
    String dni,
    Long idHotel,
    CuentaRequest cuentaBancaria
) {
    public Propietario toPropietario() {
        Propietario propietario = new Propietario();
        propietario.setNombre(this.nombre);
        propietario.setEmail(this.email);
        propietario.setTelefono(this.telefono);
        propietario.setDni(this.dni);
        if (this.cuentaBancaria != null) {
            CuentaBancaria cuenta = new CuentaBancaria();
            cuenta.setNumeroCuenta(this.cuentaBancaria.numeroCuenta());
            cuenta.setCbu(this.cuentaBancaria.cbu());
            cuenta.setAlias(this.cuentaBancaria.alias());
            cuenta.setBanco(Banco.builder().id(this.cuentaBancaria.idBanco()).build());
            propietario.setCuentaBancaria(cuenta);
            cuenta.setPropietario(propietario); // referencia bidireccional
        }
        return propietario;
    }
}
