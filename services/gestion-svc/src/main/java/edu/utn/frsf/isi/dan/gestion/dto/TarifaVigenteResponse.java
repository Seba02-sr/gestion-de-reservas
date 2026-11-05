package edu.utn.frsf.isi.dan.gestion.dto;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TarifaVigenteResponse(
    Integer id,
    Integer idTipoHabitacion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Double precioNoche) {}
