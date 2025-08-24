package edu.utn.frsf.isi.dan.user.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("PROPIETARIO")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Propietario extends Usuario {

    @OneToOne(mappedBy = "cuentaBancaria", cascade = CascadeType.ALL, orphanRemoval = true)
    private CuentaBancaria cuentaBancaria;

    @Column(name = "hotel_id")
    private Long idHotel;
}
