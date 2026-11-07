package edu.utn.frsf.isi.dan.reservas_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {
  private Integer id;
  private String nombre;
  private Integer categoria;
  private String domicilio;

  /**
   * Punto GeoJSON con coordenadas [longitud, latitud] (x = longitud, y = latitud). El índice
   * 2dsphere sobre {@code hotel.ubicacion} se crea explícitamente al arrancar ({@code
   * MongoIndexInitializer}).
   */
  private GeoJsonPoint ubicacion;
}
