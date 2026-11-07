package edu.utn.frsf.isi.dan.shared;

import java.time.LocalDate;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitacionEvent {
  private HabitacionDTO habitacion;
  private TarifaDTO tarifa;
  private TipoEvento tipoEvento;

  /** Solo para {@link TipoEvento#CERRAR_HOTEL}: id del hotel que se cierra. */
  private Integer hotelId;

  /** Solo para {@link TipoEvento#CERRAR_HOTEL}: ids de todas las habitaciones del hotel. */
  private List<Long> habitacionIds;

  /**
   * Solo para {@link TipoEvento#CERRAR_HOTEL}: día desde el cual las habitaciones están cerradas.
   */
  private LocalDate fechaCierre;
}
