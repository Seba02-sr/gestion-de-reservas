package edu.utn.frsf.isi.dan.reservas_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint; // Spring Data 3.0+
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {
  private Integer id;
  private String nombre;
  private Integer categoria;
  private String domicilio;

  @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
  private GeoJsonPoint ubicacion; // <-- NUEVO campo geoespacial
}
