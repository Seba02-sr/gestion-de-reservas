package edu.utn.frsf.isi.dan.user.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tarjetas_credito")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TarjetaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_tarjeta")
    private String numero;

    @Column(name = "nombre_titular")
    private String nombreTitular;
    @Column(name = "fecha_vencimiento")    
    private String fechaVencimiento;
    @Column(name = "codigo_seguridad")    
    private String cvc;
    @Column(name = "es_principal")    
    @Builder.Default
    private Boolean esPrincipal = false;

    @ManyToOne
    @JoinColumn(name = "banco_id")
    private Banco banco;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Huesped huesped;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Builder.Default
    private Boolean activo = true;

    public boolean isPrincipal() {
        return esPrincipal != null && esPrincipal;
    }
}
