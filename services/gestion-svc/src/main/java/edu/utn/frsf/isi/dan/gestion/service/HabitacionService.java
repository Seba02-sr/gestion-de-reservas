package edu.utn.frsf.isi.dan.gestion.service;

import edu.utn.frsf.isi.dan.gestion.dao.HabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dao.HotelRepository;
import edu.utn.frsf.isi.dan.gestion.dao.TipoHabitacionRepository;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionRequest;
import edu.utn.frsf.isi.dan.gestion.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.gestion.dto.TarifaVigenteResponse;
import edu.utn.frsf.isi.dan.gestion.mapper.HabitacionMapper;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventFactory;
import edu.utn.frsf.isi.dan.gestion.messaging.HabitacionEventoPublicado;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, modificación, baja y consulta de habitaciones. Cada alta/modificación/baja notifica a
 * reservas-svc: el evento se arma dentro de la transacción y se envía a RabbitMQ recién después del
 * commit (ver {@code HabitacionEventPublisher}).
 */
@Service
@Log4j2
public class HabitacionService {

  @Autowired private HabitacionRepository habitacionRepository;

  @Autowired private HotelRepository hotelRepository;

  @Autowired private TipoHabitacionRepository tipoHabitacionRepository;

  @Autowired private TarifaService tarifaService;

  @Autowired private HabitacionMapper habitacionMapper;

  @Autowired private HabitacionEventFactory eventFactory;

  @Autowired private ApplicationEventPublisher eventPublisher;

  @Transactional
  public HabitacionResponse crear(HabitacionRequest request) {
    Hotel hotel = getHotelOrThrow(request.idHotel());
    if (Boolean.TRUE.equals(hotel.getCerrado())) {
      throw new IllegalStateException(
          "El hotel " + hotel.getId() + " está cerrado y no admite habitaciones nuevas");
    }
    TipoHabitacion tipo = getTipoOrThrow(request.idTipoHabitacion());
    validarNumeroLibre(hotel.getId(), request.numero(), null);

    Habitacion habitacion = habitacionMapper.toEntity(request);
    habitacion.setHotel(hotel);
    habitacion.setTipoHabitacion(tipo);

    Habitacion guardada = habitacionRepository.save(habitacion);
    log.info("Habitación {} creada en el hotel {}", guardada.getId(), hotel.getId());
    eventPublisher.publishEvent(
        new HabitacionEventoPublicado(eventFactory.evento(guardada, TipoEvento.CREAR)));
    return habitacionMapper.toResponse(guardada);
  }

  @Transactional
  public HabitacionResponse actualizar(Integer id, HabitacionRequest request) {
    Habitacion habitacion = getHabitacionOrThrow(id);
    Hotel hotel = habitacion.getHotel();
    if (!hotel.getId().equals(request.idHotel())) {
      throw new IllegalArgumentException("No se puede cambiar el hotel de una habitación");
    }
    if (Boolean.TRUE.equals(request.disponible()) && Boolean.TRUE.equals(hotel.getCerrado())) {
      throw new IllegalStateException(
          "El hotel "
              + hotel.getId()
              + " está cerrado: sus habitaciones no pueden estar disponibles");
    }
    TipoHabitacion tipo = getTipoOrThrow(request.idTipoHabitacion());
    validarNumeroLibre(hotel.getId(), request.numero(), id);

    habitacionMapper.updateEntityFromRequest(request, habitacion);
    habitacion.setTipoHabitacion(tipo);

    Habitacion guardada = habitacionRepository.save(habitacion);
    eventPublisher.publishEvent(
        new HabitacionEventoPublicado(eventFactory.evento(guardada, TipoEvento.ACTUALIZAR_DATOS)));
    return habitacionMapper.toResponse(guardada);
  }

  @Transactional
  public void eliminar(Integer id) {
    Habitacion habitacion = getHabitacionOrThrow(id);
    // El evento se arma antes de borrar; se envía igualmente recién tras el commit.
    eventPublisher.publishEvent(new HabitacionEventoPublicado(eventFactory.eliminar(habitacion)));
    habitacionRepository.delete(habitacion);
    log.info("Habitación {} eliminada", id);
  }

  @Transactional(readOnly = true)
  public HabitacionResponse obtener(Integer id) {
    return habitacionMapper.toResponse(getHabitacionOrThrow(id));
  }

  /**
   * Lista habitaciones aplicando, en conjunto (AND), solo los filtros informados: capacidad del
   * tipo >= cantidadHuespedes, tipo de habitación y rango de precio de la tarifa vigente.
   */
  @Transactional(readOnly = true)
  public List<HabitacionResponse> buscar(
      Integer cantidadHuespedes, Integer tipoHabitacionId, Double precioMin, Double precioMax) {
    if (precioMin != null && precioMax != null && precioMin > precioMax) {
      throw new IllegalArgumentException("precioMin no puede ser mayor que precioMax");
    }
    LocalDate hoy = LocalDate.now();
    Specification<Habitacion> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          Join<Habitacion, TipoHabitacion> tipo = root.join("tipoHabitacion");
          if (cantidadHuespedes != null) {
            predicates.add(
                cb.greaterThanOrEqualTo(tipo.<Integer>get("capacidad"), cantidadHuespedes));
          }
          if (tipoHabitacionId != null) {
            predicates.add(cb.equal(tipo.get("id"), tipoHabitacionId));
          }
          if (precioMin != null || precioMax != null) {
            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<Tarifa> tarifa = sq.from(Tarifa.class);
            List<Predicate> vigente = new ArrayList<>();
            vigente.add(cb.equal(tarifa.get("tipoHabitacion").get("id"), tipo.get("id")));
            vigente.add(cb.lessThanOrEqualTo(tarifa.<LocalDate>get("fechaInicio"), hoy));
            vigente.add(
                cb.or(
                    cb.isNull(tarifa.get("fechaFin")),
                    cb.greaterThanOrEqualTo(tarifa.<LocalDate>get("fechaFin"), hoy)));
            if (precioMin != null) {
              vigente.add(cb.greaterThanOrEqualTo(tarifa.<Double>get("precioNoche"), precioMin));
            }
            if (precioMax != null) {
              vigente.add(cb.lessThanOrEqualTo(tarifa.<Double>get("precioNoche"), precioMax));
            }
            sq.select(tarifa.<Integer>get("id")).where(vigente.toArray(new Predicate[0]));
            predicates.add(cb.exists(sq));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return habitacionRepository.findAll(spec).stream().map(habitacionMapper::toResponse).toList();
  }

  /** Tarifa vigente hoy según el tipo de la habitación. */
  @Transactional(readOnly = true)
  public TarifaVigenteResponse obtenerTarifaVigente(Integer id) {
    Habitacion habitacion = getHabitacionOrThrow(id);
    Integer tipoId = habitacion.getTipoHabitacion().getId();
    Tarifa tarifa =
        tarifaService
            .obtenerTarifaVigente(tipoId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "No hay una tarifa vigente para el tipo de habitación " + tipoId));
    return TarifaVigenteResponse.builder()
        .id(tarifa.getId())
        .idTipoHabitacion(tipoId)
        .fechaInicio(tarifa.getFechaInicio())
        .fechaFin(tarifa.getFechaFin())
        .precioNoche(tarifa.getPrecioNoche())
        .build();
  }

  @Transactional(readOnly = true)
  public Boolean estaDisponible(Integer id) {
    return Boolean.TRUE.equals(getHabitacionOrThrow(id).getDisponible());
  }

  private void validarNumeroLibre(Integer hotelId, Integer numero, Integer idActual) {
    boolean repetido =
        idActual == null
            ? habitacionRepository.existsByHotelIdAndNumero(hotelId, numero)
            : habitacionRepository.existsByHotelIdAndNumeroAndIdNot(hotelId, numero, idActual);
    if (repetido) {
      throw new IllegalStateException(
          "Ya existe la habitación número " + numero + " en el hotel " + hotelId);
    }
  }

  private Habitacion getHabitacionOrThrow(Integer id) {
    return habitacionRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("No existe una habitación con ID " + id));
  }

  private Hotel getHotelOrThrow(Integer id) {
    return hotelRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("No existe un hotel con ID " + id));
  }

  private TipoHabitacion getTipoOrThrow(Integer id) {
    return tipoHabitacionRepository
        .findById(id)
        .orElseThrow(
            () -> new EntityNotFoundException("No existe un tipo de habitación con ID " + id));
  }
}
