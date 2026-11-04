package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelResponse;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.dto.HotelUpdateRequest;
import edu.utn.frsf.isi.dan.gestion.mapper.AmenityHotelMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.HotelMapper;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventFactory;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventoPublicado;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class HotelService {

  @Autowired private HotelRepository hotelRepository;

  @Autowired private HabitacionRepository habitacionRepository;

  @Autowired private HotelMapper hotelMapper;

  @Autowired private AmenityHotelMapper amenityHotelMapper;

  @Autowired private HabitacionEventFactory eventFactory;

  @Autowired private ApplicationEventPublisher eventPublisher;

  /**
   * El mapper ya se encarga de: 1- Inicializar habitaciones y amenities como listas vacías
   *
   * @param hotelRequest
   * @return
   */
  @Transactional
  public HotelResponse crearHotel(HotelRequest hotelRequest) {
    if (hotelRequest == null) {
      log.warn("Se intentó crear un hotel con una solicitud nula");
      throw new IllegalArgumentException("La transaccion no puede ser nula");
    }

    log.info(
        "Iniciando registro de un hotel con nombre {} y cuit {}",
        hotelRequest.nombre(),
        hotelRequest.cuit());

    try {
      Hotel hotelNuevo = hotelMapper.toEntity(hotelRequest);
      Hotel hotelGuardado = hotelRepository.save(hotelNuevo);
      return hotelMapper.toResponse(hotelGuardado);
    } catch (Exception e) {
      log.error("Error al crear el hotel: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Obtiene un hotel por su id.
   *
   * @throws EntityNotFoundException si el hotel no existe
   */
  public HotelResponse obtenerHotel(Integer id) {
    return hotelMapper.toResponse(getHotelOrThrow(id));
  }

  /**
   * Actualiza un hotel existente. Solo se pueden actualizar la categoría, el teléfono y el correo
   * de contacto. Reservas-svc se entera a través de un evento ACTUALIZAR_DATOS por cada habitación
   * del hotel (se envía después del commit).
   *
   * @param id Identificador del hotel a actualizar.
   * @param request Datos a actualizar.
   * @return HotelResponse con los datos actualizados.
   */
  @Transactional
  public HotelResponse actualizarHotel(Integer id, HotelUpdateRequest request) {
    if (request == null) {
      log.warn("Se intentó actualizar un hotel con una solicitud nula");
      throw new IllegalArgumentException("La solicitud no puede ser nula");
    }

    log.info("Iniciando actualización del hotel con ID {}", id);

    Hotel hotelExistente = getHotelOrThrow(id);
    hotelMapper.updateEntityFromRequest(request, hotelExistente);

    Hotel hotelActualizado = hotelRepository.save(hotelExistente);
    publicarActualizacionDeHabitaciones(hotelExistente);
    return hotelMapper.toResponse(hotelActualizado);
  }

  /**
   * Agrega uno o más amenities a un hotel existente. Las que el hotel ya tiene (o repetidas dentro
   * del mismo pedido) se ignoran.
   *
   * @param id Identificador del hotel al que se agregarán los amenities.
   * @param amenityRequests Lista de AmenityHotelRequest a agregar.
   * @return HotelResponse con los datos actualizados.
   */
  @Transactional
  public HotelResponse agregarAmenities(Integer id, List<AmenityHotelRequest> amenityRequests) {
    if (amenityRequests == null || amenityRequests.isEmpty()) {
      log.warn("Se intentó agregar amenities con una lista nula o vacía");
      throw new IllegalArgumentException("La lista de amenities no puede ser nula o vacía");
    }

    log.info("Iniciando la adición de amenities al hotel con ID {}", id);

    Hotel hotelExistente = getHotelOrThrow(id);
    List<AmenityHotel> amenityHotels = hotelExistente.getAmenities();
    if (amenityHotels == null) {
      amenityHotels = new ArrayList<>();
      hotelExistente.setAmenities(amenityHotels);
    }

    Set<Amenity> yaPresentes = new HashSet<>();
    amenityHotels.forEach(ah -> yaPresentes.add(ah.getAmenity()));

    boolean huboCambios = false;
    for (AmenityHotelRequest request : amenityRequests) {
      if (!yaPresentes.add(request.amenity())) {
        log.debug("El hotel {} ya tiene la amenity {}; se ignora", id, request.amenity());
        continue;
      }
      AmenityHotel amenityHotel = amenityHotelMapper.toEntity(request);
      amenityHotel.setHotel(hotelExistente);
      amenityHotels.add(amenityHotel);
      huboCambios = true;
    }

    Hotel hotelActualizado = hotelRepository.save(hotelExistente);
    if (huboCambios) {
      publicarActualizacionDeHabitaciones(hotelExistente);
    }
    return hotelMapper.toResponse(hotelActualizado);
  }

  /**
   * Elimina un amenity de un hotel existente.
   *
   * @param id Identificador del hotel.
   * @param amenityId Identificador del amenity a eliminar.
   */
  @Transactional
  public void eliminarAmenity(Integer id, Long amenityId) {
    if (id == null || amenityId == null) {
      log.warn("Se intentó eliminar un amenity con id o amenityId nulos");
      throw new IllegalArgumentException("El id del hotel y el id del amenity no pueden ser nulos");
    }

    log.info("Iniciando eliminación del amenity con ID {} del hotel con ID {}", amenityId, id);

    Hotel hotelExistente = getHotelOrThrow(id);
    AmenityHotel amenityHotelAEliminar = null;
    for (AmenityHotel ah : hotelExistente.getAmenities()) {
      if (ah.getId().equals(amenityId)) {
        amenityHotelAEliminar = ah;
        break;
      }
    }
    if (amenityHotelAEliminar == null) {
      log.warn("No se encontró un amenity con ID {} en el hotel con ID {}", amenityId, id);
      throw new EntityNotFoundException(
          "El amenity con ID " + amenityId + " no existe en el hotel " + id);
    }

    hotelExistente.getAmenities().remove(amenityHotelAEliminar);
    hotelRepository.save(hotelExistente);
    log.info("Amenity con ID {} eliminado del hotel con ID {}", amenityId, id);
    publicarActualizacionDeHabitaciones(hotelExistente);
  }

  /**
   * Cierra un hotel: lo marca como cerrado, deja todas sus habitaciones no disponibles y notifica a
   * reservas-svc (un evento CERRAR_HOTEL con las habitaciones, enviado después del commit) para que
   * cree una reserva CERRADA por habitación.
   *
   * @param id Identificador del hotel a cerrar.
   * @return HotelResponse con los datos actualizados.
   * @throws IllegalStateException si el hotel ya estaba cerrado
   */
  @Transactional
  public HotelResponse cerrarHotel(Integer id) {
    if (id == null) {
      log.warn("Se intentó cerrar un hotel con un ID nulo");
      throw new IllegalArgumentException("El ID del hotel no puede ser nulo");
    }

    log.info("Iniciando el cierre del hotel con ID {}", id);

    Hotel hotelExistente = getHotelOrThrow(id);
    if (Boolean.TRUE.equals(hotelExistente.getCerrado())) {
      throw new IllegalStateException("El hotel con ID " + id + " ya está cerrado");
    }
    hotelExistente.setCerrado(true);

    List<Habitacion> habitaciones = habitacionRepository.findByHotelId(id);
    habitaciones.forEach(h -> h.setDisponible(false));
    habitacionRepository.saveAll(habitaciones);

    Hotel hotelActualizado = hotelRepository.save(hotelExistente);
    log.info("Hotel con ID {} marcado como cerrado ({} habitaciones)", id, habitaciones.size());

    eventPublisher.publishEvent(
        new HabitacionEventoPublicado(
            eventFactory.cerrarHotel(hotelActualizado, habitaciones, LocalDate.now())));

    return hotelMapper.toResponse(hotelActualizado);
  }

  /**
   * Consulta hoteles con filtros opcionales.
   *
   * @param nombre
   * @param cuit
   * @param domicilio
   * @param categoria
   * @param amenities
   * @return
   */
  public List<HotelResponse> consultarHoteles(
      String nombre, String cuit, String domicilio, Integer categoria, List<Amenity> amenities) {
    log.info(
        "Consultando hoteles con filtros: nombre={}, cuit={}, domicilio={}, categoria={}, amenities={}",
        nombre,
        cuit,
        domicilio,
        categoria,
        amenities);
    try {
      Specification<Hotel> spec =
          (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            if (nombre != null) {
              predicates.add(
                  cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
            }
            if (cuit != null) {
              predicates.add(cb.equal(cb.lower(root.get("cuit")), cuit.toLowerCase()));
            }
            if (domicilio != null) {
              predicates.add(
                  cb.like(cb.lower(root.get("domicilio")), "%" + domicilio.toLowerCase() + "%"));
            }
            if (categoria != null) {
              predicates.add(cb.equal(root.get("categoria"), categoria));
            }
            if (amenities != null && !amenities.isEmpty()) {
              Join<Object, Object> joinAmenities = root.join("amenities");
              predicates.add(joinAmenities.get("amenity").in(amenities));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
          };
      List<Hotel> result = hotelRepository.findAll(spec);
      return result.stream().map(hotelMapper::toResponse).toList();
    } catch (Exception e) {
      log.error("Error al consultar hoteles: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Lista todos los hoteles sin filtros.
   *
   * @return
   */
  public List<HotelResponse> listarHoteles() {
    List<Hotel> hoteles = hotelRepository.findAll();
    return hoteles.stream().map(hotelMapper::toResponse).toList();
  }

  /**
   * Lista todos los amenities de un hotel.
   *
   * @param hotelId
   * @return
   */
  public List<AmenityHotelResponse> listarAmenities(Integer hotelId) {
    Hotel hotel = getHotelOrThrow(hotelId);
    return hotel.getAmenities().stream().map(amenityHotelMapper::toResponse).toList();
  }

  private Hotel getHotelOrThrow(Integer id) {
    if (id == null) {
      throw new IllegalArgumentException("El ID del hotel no puede ser nulo");
    }
    return hotelRepository
        .findById(id)
        .orElseThrow(
            () -> {
              log.warn("No se encontró un hotel con ID {}", id);
              return new EntityNotFoundException("No existe un hotel con ID " + id);
            });
  }

  /**
   * Publica un ACTUALIZAR_DATOS (con el DTO completo) por cada habitación del hotel para que
   * reservas-svc refresque su copia del hotel y de las amenities. Se envían después del commit.
   */
  private void publicarActualizacionDeHabitaciones(Hotel hotel) {
    for (Habitacion habitacion : habitacionRepository.findByHotelId(hotel.getId())) {
      eventPublisher.publishEvent(
          new HabitacionEventoPublicado(
              eventFactory.evento(habitacion, hotel, TipoEvento.ACTUALIZAR_DATOS)));
    }
  }
}
