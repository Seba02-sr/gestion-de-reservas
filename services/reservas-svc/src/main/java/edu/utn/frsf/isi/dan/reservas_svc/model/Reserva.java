package edu.utn.frsf.isi.dan.reservas_svc.model;

import java.time.Instant;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "reserva")
public class Reserva {
  @Id private String _id;

  /** Id de la habitación en gestion-svc. */
  private Long habitacionId;

  private Integer hotelId;
  private Instant createdAt;

  /** Primer día de la estadía, a las 00:00 de America/Argentina/Buenos_Aires. */
  private Instant checkIn;

  /** Día de salida; null en las reservas abiertas (CERRADA). */
  private Instant checkOut;

  private Double precioNoche;
  private Double precioTotal;

  /** null en las reservas BLOQUEADA y CERRADA (no tienen huésped). */
  private Huesped huesped;

  private List<Pago> pago;
  private Review clientReview;
  private Review hostReview;
  private EstadoReserva estadoReserva;
}
