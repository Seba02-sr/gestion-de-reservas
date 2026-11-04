package edu.utn.frsf.isi.dan.gestion.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "tarifa", schema = "tp_dan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarifa {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  /** Nombre de la tarifa (p. ej. "Tarifa base 2026" o "Promo verano"). */
  private String nombre;

  private LocalDate fechaInicio;

  /** null = tarifa abierta, sin fecha de fin. */
  private LocalDate fechaFin;

  @ManyToOne
  @JoinColumn(name = "id_tipo_habitacion")
  private TipoHabitacion tipoHabitacion;

  private Double precioNoche;

  /** true = tarifa especial/promocional (con inicio y fin); false = tarifa común. */
  @Builder.Default
  @Column(nullable = false)
  private Boolean especial = false;
}
