package edu.utn.frsf.isi.dan.reservas_svc.service;

import edu.utn.frsf.isi.dan.reservas_svc.dto.HabitacionResponse;
import edu.utn.frsf.isi.dan.reservas_svc.dto.ReservaResponse;
import edu.utn.frsf.isi.dan.reservas_svc.model.Habitacion;
import edu.utn.frsf.isi.dan.reservas_svc.model.Hotel;
import edu.utn.frsf.isi.dan.reservas_svc.model.Reserva;
import edu.utn.frsf.isi.dan.reservas_svc.util.Fechas;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Entidades → DTOs de respuesta. Las fechas se devuelven como día en la zona de negocio. */
@Component
public class ReservasMapper {

  @Autowired private Clock clock;

  public ReservasMapper() {}

  public ReservasMapper(Clock clock) {
    this.clock = clock;
  }

  public ReservaResponse toResponse(Reserva r) {
    return ReservaResponse.builder()
        .id(r.get_id())
        .habitacionId(r.getHabitacionId())
        .hotelId(r.getHotelId())
        .idHuesped(r.getHuesped() != null ? r.getHuesped().getIdUsuario() : null)
        .checkIn(Fechas.aFecha(r.getCheckIn(), clock.getZone()))
        .checkOut(Fechas.aFecha(r.getCheckOut(), clock.getZone()))
        .createdAt(r.getCreatedAt())
        .precioNoche(r.getPrecioNoche())
        .precioTotal(r.getPrecioTotal())
        .totalPagado(ReservaPolicy.totalPagado(r).doubleValue())
        .saldo(r.getPrecioTotal() == null ? null : ReservaPolicy.saldo(r).doubleValue())
        .estado(r.getEstadoReserva())
        .pagos(r.getPago())
        .clientReview(r.getClientReview())
        .hostReview(r.getHostReview())
        .build();
  }

  public HabitacionResponse toResponse(Habitacion h) {
    List<HabitacionResponse.ReservaSimpleResponse> reservas =
        h.getReservas() == null
            ? List.of()
            : h.getReservas().stream()
                .map(
                    s ->
                        HabitacionResponse.ReservaSimpleResponse.builder()
                            .reservaId(s.getReservaId())
                            .checkIn(Fechas.aFecha(s.getCheckIn(), clock.getZone()))
                            .checkOut(Fechas.aFecha(s.getCheckOut(), clock.getZone()))
                            .precioTotal(s.getPrecioTotal())
                            .estadoReserva(s.getEstadoReserva())
                            .build())
                .toList();
    return HabitacionResponse.builder()
        .habitacionId(h.getHabitacionId())
        .numero(h.getNumero())
        .piso(h.getPiso())
        .disponible(h.getDisponible() == null || h.getDisponible())
        .capacidad(h.getCapacidad())
        .precioNoche(h.getPrecioNoche())
        .amenities(h.getAmenities())
        .idTipoHabitacion(h.getIdTipoHabitacion())
        .tipoHabitacion(h.getTipoHabitacion())
        .hotel(toResponse(h.getHotel()))
        .reservas(reservas)
        .build();
  }

  private static HabitacionResponse.HotelResponse toResponse(Hotel hotel) {
    if (hotel == null) {
      return null;
    }
    return HabitacionResponse.HotelResponse.builder()
        .id(hotel.getId())
        .nombre(hotel.getNombre())
        .categoria(hotel.getCategoria())
        .domicilio(hotel.getDomicilio())
        .latitud(hotel.getUbicacion() != null ? hotel.getUbicacion().getY() : null)
        .longitud(hotel.getUbicacion() != null ? hotel.getUbicacion().getX() : null)
        .build();
  }
}
