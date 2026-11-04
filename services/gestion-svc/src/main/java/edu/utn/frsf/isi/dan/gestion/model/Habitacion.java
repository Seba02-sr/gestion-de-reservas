package edu.utn.frsf.isi.dan.gestion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "habitacion",
    schema = "tp_dan",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_habitacion_hotel_numero",
            columnNames = {"id_hotel", "numero"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habitacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private Integer numero;
  private Integer piso;

  @Builder.Default
  @Column(nullable = false)
  private Boolean disponible = true;

  @ManyToOne
  @JoinColumn(name = "id_tipo")
  private TipoHabitacion tipoHabitacion;

  @ManyToOne
  @JoinColumn(name = "id_hotel")
  private Hotel hotel;
}
