package edu.utn.frsf.isi.dan.reservas_svc.service;

import edu.utn.frsf.isi.dan.reservas_svc.dto.BloqueoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.PagoRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaRequest;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaResponse;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReviewRequest;
import edu.utn.frsf.isi.dan.reservas_svc.exception.ConflictoException;
import edu.utn.frsf.isi.dan.reservas_svc.exception.RecursoNoEncontradoException;
import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion.ReservaSimple;
import edu.utn.frsf.isi.dan.reservas_svc.model.Huesped;
import edu.utn.frsf.isi.dan.reservas_svc.model.Pago;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Review;
import edu.utn.frsf.isi.dan.reservas_svc.model.Tarifa;
import edu.utn.frsf.isi.dan.reservas_svc.repository.ReservaRepository;
import edu.utn.frsf.isi.dan.reservas_svc.util.Fechas;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Casos de uso de reservas. Las reglas puras viven en {@link ReservaPolicy}; acá se orquestan los
 * dos documentos que participan: la {@code Reserva} y la copia embebida ({@link ReservaSimple}) en
 * la habitación.
 *
 * <p>Orden de escritura: para reservar primero se toma el cupo en la habitación (operación atómica)
 * y recién después se guarda la reserva; si el guardado falla se libera el cupo. Un corte a mitad
 * de camino deja a lo sumo fechas bloqueadas de más, nunca una doble reserva.
 */
@Service
@Log4j2
public class ReservaService {

  @Autowired private ReservaRepository reservaRepository;

  @Autowired private HabitacionService habitacionService;

  @Autowired private ReservasMapper mapper;

  @Autowired private Clock clock;

  // ----------------------------------------------------------------- consultas

  public ReservaResponse obtener(String id) {
    return mapper.toResponse(getOrThrow(id));
  }

  public List<ReservaResponse> listarPorHuesped(Integer idHuesped) {
    List<Reserva> reservas =
        idHuesped == null
            ? reservaRepository.findAll()
            : reservaRepository.findByHuesped_IdUsuario(String.valueOf(idHuesped));
    return reservas.stream().map(mapper::toResponse).toList();
  }

  // --------------------------------------------------------------------- alta

  public ReservaResponse crear(ReservaRequest request) {
    LocalDate hoy = hoy();
    validarFechas(request.checkIn(), request.checkOut(), hoy);

    Habitacion habitacion = getHabitacionOrThrow(request.habitacionId());
    if (Boolean.FALSE.equals(habitacion.getDisponible())) {
      throw new ConflictoException(
          "La habitación " + request.habitacionId() + " no está disponible");
    }
    if (habitacion.getPrecioNoche() == null) {
      throw new ConflictoException("La habitación no tiene tarifa vigente");
    }

    long noches = ReservaPolicy.noches(request.checkIn(), request.checkOut());
    double precioTotal = ReservaPolicy.precioTotal(habitacion.getPrecioNoche(), noches);
    Instant in = Fechas.inicioDelDia(request.checkIn(), clock.getZone());
    Instant out = Fechas.inicioDelDia(request.checkOut(), clock.getZone());

    Reserva reserva =
        Reserva.builder()
            ._id(new ObjectId().toHexString())
            .habitacionId(request.habitacionId())
            .hotelId(habitacion.getHotel() != null ? habitacion.getHotel().getId() : null)
            .createdAt(Instant.now(clock))
            .checkIn(in)
            .checkOut(out)
            .precioNoche(habitacion.getPrecioNoche())
            .precioTotal(precioTotal)
            .huesped(Huesped.builder().idUsuario(String.valueOf(request.idHuesped())).build())
            .pago(new ArrayList<>())
            .estadoReserva(EstadoReserva.RESERVADA)
            .build();
    return guardarTomandoCupo(reserva, habitacion);
  }

  /** El dueño bloquea fechas de una habitación: reserva BLOQUEADA, sin huésped ni precio. */
  public ReservaResponse crearBloqueo(BloqueoRequest request) {
    LocalDate hoy = hoy();
    validarFechas(request.checkIn(), request.checkOut(), hoy);
    Habitacion habitacion = getHabitacionOrThrow(request.habitacionId());

    Reserva bloqueo =
        Reserva.builder()
            ._id(new ObjectId().toHexString())
            .habitacionId(request.habitacionId())
            .hotelId(habitacion.getHotel() != null ? habitacion.getHotel().getId() : null)
            .createdAt(Instant.now(clock))
            .checkIn(Fechas.inicioDelDia(request.checkIn(), clock.getZone()))
            .checkOut(Fechas.inicioDelDia(request.checkOut(), clock.getZone()))
            .estadoReserva(EstadoReserva.BLOQUEADA)
            .build();
    return guardarTomandoCupo(bloqueo, habitacion);
  }

  private ReservaResponse guardarTomandoCupo(Reserva reserva, Habitacion habitacion) {
    ReservaSimple simple =
        ReservaSimple.builder()
            .reservaId(reserva.get_id())
            .checkIn(reserva.getCheckIn())
            .checkOut(reserva.getCheckOut())
            .precioTotal(reserva.getPrecioTotal())
            .estadoReserva(reserva.getEstadoReserva())
            .build();
    // paso atómico: verifica que esté libre y agrega la reserva en la misma operación
    if (!habitacionService.tomarCupo(
        reserva.getHabitacionId(), simple, reserva.getCheckIn(), reserva.getCheckOut())) {
      throw new ConflictoException(
          "La habitación " + reserva.getHabitacionId() + " no está disponible en esas fechas");
    }
    try {
      return mapper.toResponse(reservaRepository.save(reserva));
    } catch (RuntimeException e) {
      log.error("No se pudo guardar la reserva {}; se libera el cupo", reserva.get_id(), e);
      habitacionService.liberarCupo(reserva.getHabitacionId(), reserva.get_id());
      throw e;
    }
  }

  // -------------------------------------------------------------------- pagos

  /**
   * Registra un pago. Con el primer pago la reserva pasa de RESERVADA a CONFIRMADA (regla en {@link
   * ReservaPolicy#pagoAlcanzaParaConfirmar}).
   */
  public ReservaResponse agregarPago(String id, PagoRequest request) {
    Reserva reserva = getOrThrow(id);
    ReservaPolicy.validarPago(reserva, request.monto());

    List<Pago> pagos = new ArrayList<>(reserva.getPago() == null ? List.of() : reserva.getPago());
    pagos.add(
        Pago.builder()
            .method(request.method())
            .transactionId(request.transactionId())
            .amount(new Tarifa(request.monto(), "ARS"))
            .status("COMPLETED")
            .build());
    reserva.setPago(pagos);

    EstadoReserva anterior = reserva.getEstadoReserva();
    if (anterior == EstadoReserva.RESERVADA && ReservaPolicy.pagoAlcanzaParaConfirmar(reserva)) {
      reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
    }
    Reserva guardada = reservaRepository.save(reserva);
    if (guardada.getEstadoReserva() != anterior) {
      habitacionService.sincronizarEstado(
          guardada.getHabitacionId(), guardada.get_id(), guardada.getEstadoReserva());
    }
    return mapper.toResponse(guardada);
  }

  // ------------------------------------------------------------------- estado

  /** PATCH /estado: aplica la máquina de estados ({@link ReservaPolicy#estadoTrasPedir}). */
  public ReservaResponse cambiarEstado(String id, EstadoReserva destino) {
    Reserva reserva = getOrThrow(id);
    EstadoReserva nuevo = ReservaPolicy.estadoTrasPedir(reserva, destino);

    reserva.setEstadoReserva(nuevo);
    Reserva guardada = reservaRepository.save(reserva);
    if (nuevo == EstadoReserva.CANCELADA) {
      habitacionService.liberarCupo(guardada.getHabitacionId(), guardada.get_id());
    } else {
      habitacionService.sincronizarEstado(guardada.getHabitacionId(), guardada.get_id(), nuevo);
    }
    return mapper.toResponse(guardada);
  }

  // ------------------------------------------------------------------ reviews

  /** Calificación del huésped: solo después del check-out. */
  public ReservaResponse reviewCliente(String id, ReviewRequest request) {
    Reserva reserva = getOrThrow(id);
    ReservaPolicy.validarReviewCliente(
        reserva, hoy(), Fechas.aFecha(reserva.getCheckOut(), clock.getZone()));
    reserva.setClientReview(nuevoReview(request));
    return mapper.toResponse(reservaRepository.save(reserva));
  }

  /** Review del dueño (hostReview), requisito para finalizar la reserva. */
  public ReservaResponse reviewDueno(String id, ReviewRequest request) {
    Reserva reserva = getOrThrow(id);
    ReservaPolicy.validarReviewDueno(reserva);
    reserva.setHostReview(nuevoReview(request));
    return mapper.toResponse(reservaRepository.save(reserva));
  }

  private Review nuevoReview(ReviewRequest request) {
    return Review.builder()
        .rating(request.rating())
        .comment(request.comentario())
        .createdAt(Instant.now(clock))
        .build();
  }

  // ------------------------------------------------------------------ helpers

  private LocalDate hoy() {
    return LocalDate.now(clock);
  }

  private static void validarFechas(LocalDate checkIn, LocalDate checkOut, LocalDate hoy) {
    if (!checkIn.isBefore(checkOut)) {
      throw new IllegalArgumentException("checkIn debe ser anterior a checkOut");
    }
    if (checkIn.isBefore(hoy)) {
      throw new IllegalArgumentException("checkIn no puede ser anterior a hoy (" + hoy + ")");
    }
  }

  private Reserva getOrThrow(String id) {
    return reservaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("No existe la reserva " + id));
  }

  private Habitacion getHabitacionOrThrow(Long habitacionId) {
    return habitacionService
        .findByHabitacionId(habitacionId)
        .orElseThrow(
            () -> new RecursoNoEncontradoException("No existe la habitación " + habitacionId));
  }
}
