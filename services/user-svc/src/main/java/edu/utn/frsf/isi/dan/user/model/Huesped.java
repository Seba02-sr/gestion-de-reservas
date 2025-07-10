package edu.utn.frsf.isi.dan.user.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("HUESPED")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Huesped extends Usuario {

    private LocalDate fechaNacimiento;
    @OneToMany(mappedBy = "huesped", cascade= CascadeType.ALL, orphanRemoval = true)
    private List<TarjetaCredito> tarjetaCredito;

}
