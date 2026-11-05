package edu.utn.frsf.isi.dan.reservas_svc.model;

import java.time.Instant;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Réplica local de una habitación de gestion-svc (la fuente de verdad). Se mantiene solo con los
 * mensajes de RabbitMQ; lleva embebida la lista de reservas para resolver la disponibilidad con una
 * única consulta (y una única operación atómica al reservar).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "habitacion")
public class Habitacion {
  @Id private String id;

  /** Id de la habitación en gestion-svc. */
  private Long habitacionId;

  private Integer numero;
  private Integer piso;

  /** null se interpreta como disponible. */
  private Boolean disponible;

  private Integer capacidad;
  private Double precioNoche;
  private List<String> amenities;
  private List<ReservaSimple> reservas;
  private Hotel hotel;
  private Integer idTipoHabitacion;
  private String tipoHabitacion;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ReservaSimple {
    private String reservaId;
    private Instant checkIn;

    /** null = abierta (CERRADA, BLOQUEADA sin fin). */
    private Instant checkOut;

    private Double precioTotal;
    private EstadoReserva estadoReserva;
  }
}
