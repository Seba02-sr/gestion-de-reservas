package edu.utn.frsf.isi.dan.gestion.dto;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record TarifaResponse(
    Integer id,
    String nombre,
    Integer idTipoHabitacion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    Double precioNoche,
    Boolean especial) {}
