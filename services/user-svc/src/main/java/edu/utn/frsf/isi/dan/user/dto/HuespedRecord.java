package edu.utn.frsf.isi.dan.user.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import edu.utn.frsf.isi.dan.user.model.Banco;
import edu.utn.frsf.isi.dan.user.model.Huesped;
import edu.utn.frsf.isi.dan.user.model.TarjetaCredito;

public record HuespedRecord(
    String nombre,
    String email,
    String telefono,
    String dni,
    LocalDate fechaNacimiento,
    String numeroCC,
    String nombreTitular,
    String fechaVencimientoCC,
    String cvcCC,
    Boolean esPrincipalCC,
    Integer idBanco
) {

    public Huesped toHuesped() {
        Huesped huesped = new Huesped();
        huesped.setNombre(this.nombre);
        huesped.setEmail(this.email);
        huesped.setTelefono(this.telefono);
        huesped.setDni(this.dni);
        huesped.setFechaNacimiento(this.fechaNacimiento);
        
        // Crear la tarjeta de crédito
        TarjetaCredito tarjeta = TarjetaCredito.builder()
            .numero(this.numeroCC)
            .nombreTitular(this.nombreTitular)
            .fechaVencimiento(this.fechaVencimientoCC)
            .cvc(this.cvcCC)
            .esPrincipal(this.esPrincipalCC)
            .banco(Banco.builder()
                .id(this.idBanco)
                .build())
            .huesped(huesped)
            .build();
        
        // Asignar la lista de tarjetas al huésped
        huesped.setTarjetaCredito(new ArrayList<>(List.of(tarjeta)));
        
        return huesped;
    }

    public TarjetaCredito toTarjetaCredito() {
        return TarjetaCredito.builder()
            .numero(this.numeroCC)
            .fechaVencimiento(this.fechaVencimientoCC)
            .nombreTitular(this.nombreTitular)
            .cvc(this.cvcCC)
            .esPrincipal(this.esPrincipalCC)
            .build();
    }
}