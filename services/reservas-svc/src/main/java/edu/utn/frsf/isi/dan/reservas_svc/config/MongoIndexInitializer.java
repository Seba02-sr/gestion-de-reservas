package edu.utn.frsf.isi.dan.reservas_svc.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Crea los índices al arrancar. Spring Data no los crea solo (auto-index-creation está apagado),
 * así que se piden explícitamente. Es idempotente: si ya existen no hace nada.
 *
 * <ul>
 *   <li>{@code habitacion.hotel.ubicacion}: 2dsphere, para las búsquedas por cercanía.
 *   <li>{@code habitacion.habitacionId}: único, para que CREAR sea idempotente aunque lleguen dos
 *       mensajes a la vez.
 *   <li>{@code reserva.habitacionId} y {@code reserva.huesped.idUsuario}: para las consultas.
 * </ul>
 *
 * <p>Si Mongo no responde se registra el error y el servicio arranca igual (la consulta con {@code
 * $geoWithin} funciona sin índice, solo es más lenta).
 */
@Component
@Log4j2
public class MongoIndexInitializer {

  @Autowired private MongoTemplate mongoTemplate;

  @Value("${reservas.crear-indices:true}")
  private boolean crearIndices;

  @EventListener(ApplicationReadyEvent.class)
  public void crearIndices() {
    if (!crearIndices) {
      log.info("Creación de índices deshabilitada (reservas.crear-indices=false)");
      return;
    }
    try {
      mongoTemplate
          .indexOps("habitacion")
          .ensureIndex(
              new GeospatialIndex("hotel.ubicacion")
                  .typed(GeoSpatialIndexType.GEO_2DSPHERE)
                  .named("hotel_ubicacion_2dsphere"));
      mongoTemplate
          .indexOps("habitacion")
          .ensureIndex(
              new Index("habitacionId", Sort.Direction.ASC).unique().named("habitacionId_unico"));
      mongoTemplate
          .indexOps("reserva")
          .ensureIndex(new Index("habitacionId", Sort.Direction.ASC).named("reserva_habitacionId"));
      mongoTemplate
          .indexOps("reserva")
          .ensureIndex(
              new Index("huesped.idUsuario", Sort.Direction.ASC)
                  .named("reserva_huesped_idUsuario"));
      log.info(
          "Índices de MongoDB verificados (2dsphere hotel.ubicacion, habitacionId único, reserva)");
    } catch (Exception e) {
      log.error("No se pudieron crear los índices de MongoDB: {}", e.getMessage(), e);
    }
  }
}
