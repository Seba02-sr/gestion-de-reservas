package edu.utn.frsf.isi.dan.reservas_svc.dto;

import edu.utn.frsf.isi.dan.reservas_svc.model.EstadoReserva;
import edu.utn.frsf.isi.dan.reservas_svc.model.Pago;
import edu.utn.frsf.isi.dan.reservas_svc.model.Review;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record ReservaResponse(
    String id,
    Long habitacionId,
    Integer hotelId,
    String idHuesped,
    LocalDate checkIn,
    LocalDate checkOut,
    Instant createdAt,
    Double precioNoche,
    Double precioTotal,
    Double totalPagado,
    Double saldo,
    EstadoReserva estado,
    List<Pago> pagos,
    Review clientReview,
    Review hostReview) {}
