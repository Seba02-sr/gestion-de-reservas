package edu.utn.frsf.isi.dan.reservas_svc.service;

import edu.utn.frsf.isi.dan.reservas_svc.dto.BusquedaHabitacionesRequest;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion.ReservaSimple;
import edu.utn.frsf.isi.dan.reservas_svc.model.Hotel;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.repository.HabitacionRepository;
import edu.utn.frsf.isi.dan.reservas_svc.util.Fechas;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Réplica local de habitaciones: se mantiene SOLO con los eventos de gestion-svc (nunca por HTTP),
 * y resuelve las operaciones atómicas sobre la lista de reservas embebida y la búsqueda de
 * disponibilidad.
 */
@Service
@Log4j2
public class HabitacionService {
  @Autowired private HabitacionRepository habitacionRepository;

  @Autowired private MongoTemplate mongoTemplate;

  @Autowired private Clock clock;

  public List<Habitacion> findAll() {
    return habitacionRepository.findAll();
  }

  // ------------------------------------------------------------------ eventos

  public void handleEvent(HabitacionEvent event) {
    switch (event.getTipoEvento()) {
      case CREAR:
        crearIdempotente(mapFromHabitacion(event.getHabitacion()));
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
        cerrarHotel(event);
        break;
      default:
        throw new IllegalArgumentException("Tipo de evento desconocido: " + event.getTipoEvento());
    }
  }

  public Habitacion mapFromHabitacion(HabitacionDTO dto) {
    return Habitacion.builder()
        .habitacionId(dto.getHabitacionId())
        .numero(dto.getNumero())
        .piso(dto.getPiso())
        .disponible(dto.getDisponible())
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
    // GeoJSON: [longitud, latitud] (x = longitud, y = latitud)
    GeoJsonPoint ubicacion =
        dto.getLatitud() != null && dto.getLongitud() != null
            ? new GeoJsonPoint(dto.getLongitud(), dto.getLatitud())
            : null;
    return Hotel.builder()
        .id(dto.getId())
        .nombre(dto.getNombre())
        .domicilio(dto.getDomicilio())
        .categoria(dto.getCategoria())
        .ubicacion(ubicacion)
        .build();
  }

  public Optional<Habitacion> findByHabitacionId(Long habitacionId) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
    Habitacion habitacion = mongoTemplate.findOne(query, Habitacion.class);
    return Optional.ofNullable(habitacion);
  }

  /**
   * CREAR idempotente: upsert por {@code habitacionId}. Si el mensaje llega dos veces (o dos hilos
   * lo procesan a la vez) queda una sola habitación; {@code reservas} solo se inicializa al
   * insertar, así un duplicado nunca borra reservas ya tomadas.
   */
  public void crearIdempotente(Habitacion habitacion) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacion.getHabitacionId()));
    Update update = datos(habitacion).setOnInsert("reservas", new ArrayList<ReservaSimple>());
    mongoTemplate.upsert(query, update, Habitacion.class);
  }

  public Habitacion updateByHabitacionId(Long habitacionId, Habitacion nuevaHabitacion) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
    Habitacion actualizada =
        mongoTemplate.findAndModify(
            query,
            datos(nuevaHabitacion),
            FindAndModifyOptions.options().returnNew(true),
            Habitacion.class);
    if (actualizada == null) {
      throw new IllegalArgumentException(
          "No se encontró la habitación con habitacionId: " + habitacionId);
    }
    return actualizada;
  }

  /**
   * Campos que vienen de gestion-svc (el precio se pisa aunque sea null: significa "sin tarifa").
   */
  private static Update datos(Habitacion h) {
    Update update =
        new Update()
            .set("precioNoche", h.getPrecioNoche())
            .set("capacidad", h.getCapacidad())
            .set("amenities", h.getAmenities())
            .set("idTipoHabitacion", h.getIdTipoHabitacion())
            .set("tipoHabitacion", h.getTipoHabitacion());
    if (h.getNumero() != null) {
      update.set("numero", h.getNumero());
    }
    if (h.getPiso() != null) {
      update.set("piso", h.getPiso());
    }
    if (h.getDisponible() != null) {
      update.set("disponible", h.getDisponible());
    }
    if (h.getHotel() != null) {
      // el hotel (datos y ubicación) también se refresca cuando cambia en gestion-svc
      update.set("hotel", h.getHotel());
    }
    return update;
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

  /**
   * CERRAR_HOTEL: por cada habitación crea una reserva CERRADA (check-in = fecha de cierre, sin
   * check-out) y la deja no disponible. Idempotente: si la habitación ya tiene una reserva CERRADA
   * (mensaje repetido) no hace nada; si no existe en la réplica, la saltea.
   */
  public void cerrarHotel(HabitacionEvent event) {
    List<Long> ids = event.getHabitacionIds();
    if (ids == null || ids.isEmpty()) {
      log.info("CERRAR_HOTEL del hotel {} sin habitaciones", event.getHotelId());
      return;
    }
    LocalDate fecha =
        event.getFechaCierre() != null ? event.getFechaCierre() : LocalDate.now(clock);
    Instant desde = Fechas.inicioDelDia(fecha, clock.getZone());

    for (Long habitacionId : ids) {
      String reservaId = new ObjectId().toHexString();
      Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
      // atómico: solo matchea si todavía no tiene una reserva CERRADA
      query.addCriteria(
          new Criteria()
              .norOperator(
                  Criteria.where("reservas")
                      .elemMatch(
                          Criteria.where("estadoReserva").is(EstadoReserva.CERRADA.name()))));
      Update update =
          new Update()
              .push(
                  "reservas",
                  ReservaSimple.builder()
                      .reservaId(reservaId)
                      .checkIn(desde)
                      .checkOut(null)
                      .estadoReserva(EstadoReserva.CERRADA)
                      .build())
              .set("disponible", false);
      Habitacion previa = mongoTemplate.findAndModify(query, update, Habitacion.class);
      if (previa == null) {
        log.info(
            "CERRAR_HOTEL: la habitación {} ya estaba cerrada o no existe en la réplica; se omite",
            habitacionId);
        continue;
      }
      mongoTemplate.insert(
          Reserva.builder()
              ._id(reservaId)
              .habitacionId(habitacionId)
              .hotelId(event.getHotelId())
              .createdAt(Instant.now(clock))
              .checkIn(desde)
              .checkOut(null)
              .estadoReserva(EstadoReserva.CERRADA)
              .build());
    }
    log.info(
        "CERRAR_HOTEL del hotel {} procesado ({} habitaciones)", event.getHotelId(), ids.size());
  }

  // ------------------------------------------------- operaciones sobre reservas

  /**
   * Toma las fechas de una habitación de forma ATÓMICA: una sola operación de Mongo (sobre un único
   * documento) que verifica que la habitación esté disponible y libre en el pedido y, en ese mismo
   * paso, agrega la reserva. Si dos pedidos compiten por las mismas fechas solo uno modifica el
   * documento; el otro recibe {@code false}.
   */
  public boolean tomarCupo(Long habitacionId, ReservaSimple reserva, Instant in, Instant out) {
    Query query =
        new Query(
            new Criteria()
                .andOperator(
                    Criteria.where("habitacionId").is(habitacionId),
                    Criteria.where("disponible").ne(false),
                    Disponibilidad.libre(in, out)));
    Update update = new Update().push("reservas", reserva);
    return mongoTemplate.findAndModify(query, update, Habitacion.class) != null;
  }

  /** Quita la reserva de la lista de la habitación (cancelación o compensación). */
  public void liberarCupo(Long habitacionId, String reservaId) {
    Query query = new Query(Criteria.where("habitacionId").is(habitacionId));
    Update update =
        new Update()
            .pull(
                "reservas",
                Query.query(Criteria.where("reservaId").is(reservaId)).getQueryObject());
    mongoTemplate.updateFirst(query, update, Habitacion.class);
  }

  /** Mantiene sincronizado el estado de la copia embebida de la reserva. */
  public void sincronizarEstado(Long habitacionId, String reservaId, EstadoReserva estado) {
    Query query =
        new Query(
            Criteria.where("habitacionId")
                .is(habitacionId)
                .and("reservas.reservaId")
                .is(reservaId));
    Update update = new Update().set("reservas.$.estadoReserva", estado.name());
    mongoTemplate.updateFirst(query, update, Habitacion.class);
  }

  // ----------------------------------------------------------------- búsqueda

  public List<Habitacion> buscarDisponibles(BusquedaHabitacionesRequest filtros) {
    return mongoTemplate.find(construirQuery(filtros), Habitacion.class);
  }

  /** Arma el criterio de búsqueda (todos los filtros opcionales, combinados con AND). */
  public Query construirQuery(BusquedaHabitacionesRequest f) {
    validar(f);
    List<Criteria> criterios = new ArrayList<>();
    criterios.add(Criteria.where("disponible").ne(false));

    if (f.checkIn() != null) {
      criterios.add(
          Disponibilidad.libre(
              Fechas.inicioDelDia(f.checkIn(), clock.getZone()),
              Fechas.inicioDelDia(f.checkOut(), clock.getZone())));
    }
    if (f.hotelId() != null) {
      criterios.add(Criteria.where("hotel.id").is(f.hotelId()));
    }
    if (f.cantidadHuespedes() != null) {
      criterios.add(Criteria.where("capacidad").gte(f.cantidadHuespedes()));
    }
    if (f.precioMin() != null || f.precioMax() != null) {
      Criteria precio = Criteria.where("precioNoche");
      if (f.precioMin() != null) {
        precio = precio.gte(f.precioMin());
      }
      if (f.precioMax() != null) {
        precio = precio.lte(f.precioMax());
      }
      criterios.add(precio);
    }
    if (f.estrellas() != null) {
      criterios.add(Criteria.where("hotel.categoria").gte(f.estrellas()));
    }
    if (f.amenities() != null && !f.amenities().isEmpty()) {
      List<String> requeridas =
          f.amenities().stream()
              .map(a -> a.trim().toUpperCase(Locale.ROOT))
              .filter(a -> !a.isEmpty())
              .toList();
      if (!requeridas.isEmpty()) {
        criterios.add(Criteria.where("amenities").all(requeridas));
      }
    }
    if (f.lat() != null) {
      // $geoWithin + $centerSphere: se puede combinar con el resto de los filtros (a diferencia de
      // $near). Point(x = longitud, y = latitud); el radio en km se normaliza a radianes.
      criterios.add(
          Criteria.where("hotel.ubicacion")
              .withinSphere(
                  new Circle(
                      new Point(f.lng(), f.lat()), new Distance(f.radioKm(), Metrics.KILOMETERS))));
    }
    return new Query(new Criteria().andOperator(criterios));
  }

  private static void validar(BusquedaHabitacionesRequest f) {
    if ((f.checkIn() == null) != (f.checkOut() == null)) {
      throw new IllegalArgumentException("checkIn y checkOut se informan juntos");
    }
    if (f.checkIn() != null && !f.checkIn().isBefore(f.checkOut())) {
      throw new IllegalArgumentException("checkIn debe ser anterior a checkOut");
    }
    if (f.precioMin() != null && f.precioMax() != null && f.precioMin() > f.precioMax()) {
      throw new IllegalArgumentException("precioMin no puede ser mayor que precioMax");
    }
    boolean algunaGeo = f.lat() != null || f.lng() != null || f.radioKm() != null;
    boolean todasGeo = f.lat() != null && f.lng() != null && f.radioKm() != null;
    if (algunaGeo && !todasGeo) {
      throw new IllegalArgumentException("lat, lng y radioKm se informan juntos");
    }
  }
}
