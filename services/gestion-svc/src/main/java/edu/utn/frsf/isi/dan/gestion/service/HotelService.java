package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dto.AmenityHotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HotelResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.AmenityHotelMapper;
import edu.utn.frsf.isi.dan.gestion.mapper.HotelMapper;
import edu.utn.frsf.isi.dan.gestion.model.Amenity;
import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.ArrayList;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class HotelService {

  @Autowired private HotelRepository hotelRepository;

  @Autowired private HotelMapper hotelMapper;

  @Autowired private AmenityHotelMapper amenityHotelMapper;

  /**
   * El mapper ya se encarga de:
   * 1- Inicializar habitaciones y amenities como listas vacías
   * 
   * @param hotelRequest
   * @return
   */
  @Transactional
  public HotelResponse crearHotel(HotelRequest hotelRequest) {
    if(hotelRequest == null) {
      log.warn("Se intentó crear un hotel con una solicitud nula");
      throw new IllegalArgumentException("La transaccion no puede ser nula");
    }

    log.info("Iniciando registro de un hotel con nombre {} y cuit {}", hotelRequest.nombre(), hotelRequest.cuit());
    
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
   * Actualiza un hotel existente con los datos permitidos.
   * Solo se pueden actualizar la categoría, el teléfono y el correo de contacto.
   * 
   * @param id Identificador del hotel a actualizar.
   * @param hotelRequest Datos a actualizar.
   * @return HotelResponse con los datos actualizados.
   */
  @Transactional
  public HotelResponse actualizarHotel(Integer id, HotelRequest hotelRequest) {
    if (hotelRequest == null) {
      log.warn("Se intentó actualizar un hotel con una solicitud nula");
      throw new IllegalArgumentException("La solicitud no puede ser nula");
    }

    log.info("Iniciando actualización del hotel con ID {}", id);

    Optional<Hotel> optionalHotel = hotelRepository.findById(id);
    if (optionalHotel.isEmpty()) {
      log.warn("No se encontró un hotel con ID {}", id);
      throw new IllegalArgumentException("El hotel con el ID especificado no existe");
    }

    try {
      Hotel hotelExistente = optionalHotel.get();
      // Verificar campos que NO pueden ser modificados
      if (!hotelExistente.getNombre().equals(hotelRequest.nombre()) ||
          !hotelExistente.getCuit().equals(hotelRequest.cuit()) ||
          !hotelExistente.getDomicilio().equals(hotelRequest.domicilio()) ||
          !hotelExistente.getLatitud().equals(hotelRequest.latitud()) ||
          !hotelExistente.getLongitud().equals(hotelRequest.longitud())) {
        log.warn("Intento de modificar campos no permitidos en el hotel con ID {}", id);
        throw new IllegalArgumentException("Solo se pueden modificar la categoría, el teléfono y el correo de contacto");
      }

      hotelExistente.setCategoria(hotelRequest.categoria());
      hotelExistente.setTelefono(hotelRequest.telefono());
      hotelExistente.setCorreoContacto(hotelRequest.correoContacto());

      Hotel hotelActualizado = hotelRepository.save(hotelExistente);
      return hotelMapper.toResponse(hotelActualizado);
    } catch (Exception e) {
      log.error("Error al actualizar el hotel: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Agrega uno o más amenities a un hotel existente.
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

    Optional<Hotel> optionalHotel = hotelRepository.findById(id);
    if (optionalHotel.isEmpty()) {
      log.warn("No se encontró un hotel con ID {}", id);
      throw new IllegalArgumentException("El hotel con el ID especificado no existe");
    }

    try {
      Hotel hotelExistente = optionalHotel.get();
      List<AmenityHotel> amenityHotels = hotelExistente.getAmenities();

      for (AmenityHotelRequest request : amenityRequests) {
          AmenityHotel amenityHotel = amenityHotelMapper.toEntity(request);
          amenityHotel.setHotel(hotelExistente);
          amenityHotels.add(amenityHotel);
      }

      hotelExistente.setAmenities(amenityHotels);
      Hotel hotelActualizado = hotelRepository.save(hotelExistente);
      return hotelMapper.toResponse(hotelActualizado);
    } catch (Exception e) {
      log.error("Error al agregar amenities al hotel: {}", e.getMessage(), e);
      throw e;
    }
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

    try {
      Optional<Hotel> optionalHotel = hotelRepository.findById(id);
      if (optionalHotel.isEmpty()) {
        log.warn("No se encontró un hotel con ID {}", id);
        throw new IllegalArgumentException("El hotel con el ID especificado no existe");
      }

      Hotel hotelExistente = optionalHotel.get();
      AmenityHotel amenityHotelAEliminar = null;
      for (AmenityHotel ah : hotelExistente.getAmenities()) {
        if (ah.getId().equals(amenityId)) {
          amenityHotelAEliminar = ah;
          break;
        }
      }
      if (amenityHotelAEliminar == null) {
        log.warn("No se encontró un amenity con ID {} en el hotel con ID {}", amenityId, id);
        throw new IllegalArgumentException("El amenity con el ID especificado no existe en el hotel");
      }

      hotelExistente.getAmenities().remove(amenityHotelAEliminar);
      hotelRepository.save(hotelExistente);
      log.info("Amenity con ID {} eliminado del hotel con ID {}", amenityId, id);
    } catch (Exception e) {
      log.error("Error al eliminar el amenity: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Marca un hotel como cerrado.
   * 
   * @param id Identificador del hotel a cerrar.
   * @return HotelResponse con los datos actualizados.
   */
  @Transactional
  public HotelResponse cerrarHotel(Integer id) {
    if (id == null) {
        log.warn("Se intentó cerrar un hotel con un ID nulo");
        throw new IllegalArgumentException("El ID del hotel no puede ser nulo");
    }

    log.info("Iniciando el cierre del hotel con ID {}", id);

    try {
      Optional<Hotel> optionalHotel = hotelRepository.findById(id);
      if (optionalHotel.isEmpty()) {
        log.warn("No se encontró un hotel con ID {}", id);
        throw new IllegalArgumentException("El hotel con el ID especificado no existe");
      }

      Hotel hotelExistente = optionalHotel.get();
      hotelExistente.setCerrado(true);

      Hotel hotelActualizado = hotelRepository.save(hotelExistente);
      log.info("Hotel con ID {} marcado como cerrado", id);

      /*
       * Falta agregar la siguiente lógica:
       * Marcar como no disponible todas las habitaciones del hotel
       * Se envia un mensaje a reservas-svc que el hotel cierra
       * Se crea una reserva del tipo CERRADO, para todas las habitaciones con fecha de inicio de hoy y fecha final null
       */

      return hotelMapper.toResponse(hotelActualizado);
    } catch (Exception e) {
      log.error("Error al cerrar el hotel: {}", e.getMessage(), e);
      throw e;
    }
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
  public List<HotelResponse> consultarHoteles(String nombre, String cuit, String domicilio, Integer categoria, List<Amenity> amenities) {
    log.info("Consultando hoteles con filtros: nombre={}, cuit={}, domicilio={}, categoria={}, amenities={}", nombre, cuit, domicilio, categoria, amenities);
    try {
      Specification<Hotel> spec = (root, query, cb) -> {
        query.distinct(true);
        List<Predicate> predicates = new ArrayList<>();
        if (nombre != null) {
          predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
        }
        if (cuit != null) {
          predicates.add(cb.equal(cb.lower(root.get("cuit")), cuit.toLowerCase()));
        }
        if (domicilio != null) {
          predicates.add(cb.like(cb.lower(root.get("domicilio")), "%" + domicilio.toLowerCase() + "%"));
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

}
