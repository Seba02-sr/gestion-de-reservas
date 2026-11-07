package edu.utn.frsf.isi.dan.gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "hotel", schema = "tp_dan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String nombre;
  private String cuit;
  private String domicilio;
  private Double latitud;
  private Double longitud;
  private String telefono;
  private String correoContacto;
  private Integer categoria;
  private Boolean cerrado;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "hotel")
  @JsonIgnore
  private List<Habitacion> habitaciones;

  @OneToMany(
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      mappedBy = "hotel",
      orphanRemoval = true)
  private List<AmenityHotel> amenities;
}
