package edu.utn.frsf.isi.dan.user.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuentas_bancarias")
@Data
@NoArgsConstructor
public class CuentaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_cuenta")
    private String numeroCuenta;
    private String cbu;
    private String alias;

    @ManyToOne
    @JoinColumn(name = "banco_id")
    private Banco banco;

    @OneToOne(mappedBy = "cuentaBancaria")
    private Propietario propietario;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;
}
