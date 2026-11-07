package edu.utn.frsf.isi.dan.gestion.dto;

import lombok.Builder;

@Builder
public record HabitacionResponse(
    Integer id,
    Integer numero,
    Integer piso,
    Boolean disponible,
    Integer idTipoHabitacion,
    String tipoHabitacion,
    Integer idHotel) {}
