package edu.utn.frsf.isi.dan.gestion.messaging;

import edu.utn.frsf.isi.dan.gestion.model.AmenityHotel;
import edu.utn.frsf.isi.dan.gestion.model.Habitacion;
import edu.utn.frsf.isi.dan.gestion.model.Hotel;
import edu.utn.frsf.isi.dan.gestion.model.Tarifa;
import edu.utn.frsf.isi.dan.gestion.model.TipoHabitacion;
import edu.utn.frsf.isi.dan.gestion.service.TarifaService;
import edu.utn.frsf.isi.dan.shared.HabitacionDTO;
import edu.utn.frsf.isi.dan.shared.HabitacionEvent;
import edu.utn.frsf.isi.dan.shared.HotelDTO;
import edu.utn.frsf.isi.dan.shared.TipoEvento;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Arma los {@link HabitacionEvent} que se envían a reservas-svc. Debe invocarse dentro de la
 * transacción (lee relaciones lazy como las amenities del hotel).
 */
@Component
public class HabitacionEventFactory {

  private final TarifaService tarifaService;

  public HabitacionEventFactory(TarifaService tarifaService) {
    this.tarifaService = tarifaService;
  }

  /** Evento con el {@link HabitacionDTO} completo, usando el hotel de la habitación. */
  public HabitacionEvent evento(Habitacion habitacion, TipoEvento tipo) {
    return evento(habitacion, habitacion.getHotel(), tipo);
  }

  /** Evento con el {@link HabitacionDTO} completo, usando el hotel indicado (ya actualizado). */
  public HabitacionEvent evento(Habitacion habitacion, Hotel hotel, TipoEvento tipo) {
    return HabitacionEvent.builder()
        .tipoEvento(tipo)
        .hotelId(hotel != null ? hotel.getId() : null)
        .habitacion(habitacionDto(habitacion, hotel))
        .build();
  }

  /** Evento de baja: alcanza con el id de la habitación (y el del hotel). */
  public HabitacionEvent eliminar(Habitacion habitacion) {
    Hotel hotel = habitacion.getHotel();
    return HabitacionEvent.builder()
        .tipoEvento(TipoEvento.ELIMINAR)
        .hotelId(hotel != null ? hotel.getId() : null)
        .habitacion(HabitacionDTO.builder().habitacionId(habitacion.getId().longValue()).build())
        .build();
  }

  /** Un único evento con todas las habitaciones del hotel que se cierra. */
  public HabitacionEvent cerrarHotel(Hotel hotel, List<Habitacion> habitaciones, LocalDate hoy) {
    return HabitacionEvent.builder()
        .tipoEvento(TipoEvento.CERRAR_HOTEL)
        .hotelId(hotel.getId())
        .habitacionIds(habitaciones.stream().map(h -> h.getId().longValue()).toList())
        .fechaCierre(hoy)
        .build();
  }

  HabitacionDTO habitacionDto(Habitacion habitacion, Hotel hotel) {
    TipoHabitacion tipo = habitacion.getTipoHabitacion();
    return HabitacionDTO.builder()
        .habitacionId(habitacion.getId().longValue())
        .numero(habitacion.getNumero())
        .piso(habitacion.getPiso())
        .disponible(habitacion.getDisponible())
        .tipoHabitacionId(tipo.getId())
        .tipoHabitacion(tipo.getNombre())
        .tipoHabitacionDescripcion(tipo.getDescripcion())
        .capacidad(tipo.getCapacidad())
        .precioNoche(
            tarifaService
                .obtenerTarifaVigente(tipo.getId())
                .map(Tarifa::getPrecioNoche)
                .orElse(null))
        .amenities(amenities(hotel))
        .hotel(hotelDto(hotel))
        .build();
  }

  static List<String> amenities(Hotel hotel) {
    if (hotel == null || hotel.getAmenities() == null) {
      return List.of();
    }
    return hotel.getAmenities().stream()
        .map(AmenityHotel::getAmenity)
        .map(Enum::name)
        .distinct()
        .toList();
  }

  static HotelDTO hotelDto(Hotel hotel) {
    if (hotel == null) {
      return null;
    }
    return HotelDTO.builder()
        .id(hotel.getId())
        .nombre(hotel.getNombre())
        .cuit(hotel.getCuit())
        .domicilio(hotel.getDomicilio())
        .latitud(hotel.getLatitud())
        .longitud(hotel.getLongitud())
        .telefono(hotel.getTelefono())
        .correoContacto(hotel.getCorreoContacto())
        .categoria(hotel.getCategoria())
        .build();
  }
}
