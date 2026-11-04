package edu.utn.frsf.isi.dan.reservas_svc.service;

import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Hotel;
import edu.utn.frsf.isi.dan.reservas_svc.repository.HabitacionRepository;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class HabitacionService {
  @Autowired private HabitacionRepository habitacionRepository;

  @Autowired private MongoTemplate mongoTemplate;

  public List<Habitacion> findAll() {
    return habitacionRepository.findAll();
  }

  public Optional<Habitacion> findById(String id) {
    return habitacionRepository.findById(id);
  }

  public Habitacion save(Habitacion habitacion) {
    return habitacionRepository.save(habitacion);
  }

  public void deleteById(String id) {
    habitacionRepository.deleteById(id);
  }

  public void handleEvent(HabitacionEvent event) {
    switch (event.getTipoEvento()) {
      case CREAR:
        save(mapFromHabitacion(event.getHabitacion()));
        break;
      case ACTUALIZAR_DATOS:
        updateByHabitacionId(
            event.getHabitacion().getHabitacionId(), mapFromHabitacion(event.getHabitacion()));
        break;
      case ACTUALIZAR_PRECIO:
        // TarifaDTO trae el tipo de habitación y su nuevo precio por noche
        actualizarPrecioPorTipo(
            event.getTarifa().getTipoHabitacionId(), event.getTarifa().getNuevoPrecio());
        break;
      case ELIMINAR:
        deleteByHabitacionId(event.getHabitacion().getHabitacionId());
        break;
      case CERRAR_HOTEL:
        // Pendiente (tanda de reservas-svc): crear una reserva CERRADA por cada habitación
        // (checkIn = fechaCierre, checkOut = null). Por ahora solo se confirma el mensaje.
        log.info(
            "CERRAR_HOTEL recibido para hotel {} ({} habitaciones, desde {}); sin procesar todavía",
            event.getHotelId(),
            event.getHabitacionIds() == null ? 0 : event.getHabitacionIds().size(),
            event.getFechaCierre());
        break;
      default:
        throw new IllegalArgumentException("Tipo de evento desconocido: " + event.getTipoEvento());
    }
  }

  public Habitacion mapFromHabitacion(HabitacionDTO dto) {
    return Habitacion.builder()
        .habitacionId(dto.getHabitacionId())
        .precioNoche(dto.getPrecioNoche())
        .capacidad(dto.getCapacidad())
        .amenities(dto.getAmenities())
        .idTipoHabitacion(dto.getTipoHabitacionId())
        .tipoHabitacion(dto.getTipoHabitacion())
        .hotel(mapFromDto(dto.getHotel()))
        .build();
  }

  public Hotel mapFromDto(HotelDTO dto) {
    if (dto == null) {
      return null;
    }
    return Hotel.builder()
        .id(dto.getId())
        .nombre(dto.getNombre())
        .domicilio(dto.getDomicilio())
        .categoria(dto.getCategoria())
        .ubicacion(new GeoJsonPoint(dto.getLatitud(), dto.getLongitud()))
        .build();
  }

  public Optional<Habitacion> findByHabitacionId(Long habitacionId) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
    Habitacion habitacion = mongoTemplate.findOne(query, Habitacion.class);
    return Optional.ofNullable(habitacion);
  }

  public Habitacion updateByHabitacionId(Long habitacionId, Habitacion nuevaHabitacion) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
    Update update =
        new Update()
            .set("precioNoche", nuevaHabitacion.getPrecioNoche())
            .set("capacidad", nuevaHabitacion.getCapacidad())
            .set("amenities", nuevaHabitacion.getAmenities())
            .set("idTipoHabitacion", nuevaHabitacion.getIdTipoHabitacion())
            .set("tipoHabitacion", nuevaHabitacion.getTipoHabitacion());
    if (nuevaHabitacion.getHotel() != null) {
      // el hotel (datos y ubicación) también se refresca cuando cambia en gestion-svc
      update.set("hotel", nuevaHabitacion.getHotel());
    }
    Habitacion actualizada =
        mongoTemplate.findAndModify(
            query, update, FindAndModifyOptions.options().returnNew(true), Habitacion.class);
    if (actualizada == null) {
      throw new IllegalArgumentException(
          "No se encontró la habitación con habitacionId: " + habitacionId);
    }
    return actualizada;
  }

  /** Actualiza el precioNoche de todas las habitaciones de un tipo de habitación. */
  public void actualizarPrecioPorTipo(Integer tipoHabitacionId, Double nuevoPrecio) {
    if (tipoHabitacionId == null || nuevoPrecio == null) {
      throw new IllegalArgumentException(
          "ACTUALIZAR_PRECIO requiere tipoHabitacionId y nuevoPrecio");
    }
    Query query = new Query(Criteria.where("idTipoHabitacion").is(tipoHabitacionId));
    var resultado =
        mongoTemplate.updateMulti(
            query, new Update().set("precioNoche", nuevoPrecio), Habitacion.class);
    log.info(
        "Precio {} aplicado a {} habitación(es) del tipo {}",
        nuevoPrecio,
        resultado.getModifiedCount(),
        tipoHabitacionId);
  }

  public void deleteByHabitacionId(Long habitacionId) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
    mongoTemplate.remove(query, Habitacion.class);
  }
}
