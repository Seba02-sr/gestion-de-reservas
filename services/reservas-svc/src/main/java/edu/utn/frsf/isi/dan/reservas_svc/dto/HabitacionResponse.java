package edu.utn.frsf.isi.dan.reservas_svc.dto;

import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record HabitacionResponse(
    Long habitacionId,
    Integer numero,
    Integer piso,
    Boolean disponible,
    Integer capacidad,
    Double precioNoche,
    List<String> amenities,
    Integer idTipoHabitacion,
    String tipoHabitacion,
    HotelResponse hotel,
    List<ReservaSimpleResponse> reservas) {

  @Builder
  public record HotelResponse(
      Integer id,
      String nombre,
      Integer categoria,
      String domicilio,
      Double latitud,
      Double longitud) {}

  @Builder
  public record ReservaSimpleResponse(
      String reservaId,
      LocalDate checkIn,
      LocalDate checkOut,
      Double precioTotal,
      EstadoReserva estadoReserva) {}
}
