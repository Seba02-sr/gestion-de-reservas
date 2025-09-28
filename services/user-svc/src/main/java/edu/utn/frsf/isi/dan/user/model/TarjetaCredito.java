package edu.utn.frsf.isi.dan.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.utn.frsf.isi.dan.user.util.AuditableEntity;
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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tarjetas_credito")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarjetaCredito extends AuditableEntity {

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

  public boolean isPrincipal() {
    return esPrincipal != null && esPrincipal;
  }
}
