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

  private LocalDate fechaInicio;
  private LocalDate fechaFin;

  @ManyToOne
  @JoinColumn(name = "id_tipo_habitacion")
  private TipoHabitacion tipoHabitacion;

  private Double precioNoche;
}
